package pt.ulisboa.tecnico.classes.classserver;

import com.google.common.primitives.Longs;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassStudent;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ReplicaManager {

    private static boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private boolean active;
    private boolean activeGossip;
    private Class studentClass;
    private boolean primary;
    private String host;
    private int port;
    private Timestamp valueTimestamp = new Timestamp();                  /* version timestamp of the class */
    private Timestamp replicaTimestamp = new Timestamp();                /* version timestamp of the update log */
    private Map<Timestamp, LogRecord> log = new ConcurrentHashMap<>();   /* log of received update operations */

    /* to have rough estimates of which updates are known by other replica */
    private Map<String, Timestamp> tableTimestamp = new ConcurrentHashMap<>();

    /* operations executed in this replica */
    private Set<Timestamp> executedOperations = ConcurrentHashMap.newKeySet();

    /* operations that have been executed in this replica but have been rolled back
    due to conflicting operations that came from other replica */
    private Set<Timestamp> suspendedOperations = ConcurrentHashMap.newKeySet();

    ReentrantReadWriteLock timestampLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    public ReplicaManager(String primary, String host, int port) {
        this.studentClass = new Class();
        this.active = true;
        this.activeGossip = true;
        this.primary = primary.equals("P");
        this.studentClass = new Class();
        this.host = host;
        this.port = port;
        this.valueTimestamp.put(host + ":" + port, 0);
        this.replicaTimestamp.put(host + ":" + port, 0);
        this.tableTimestamp.put(host + ":" + port, replicaTimestamp);
    }

    /**
     * Set server availability
     *
     * @param active
     */
    public void setActive(boolean active) {
        DebugMessage.debug("Server is now " + (active ? "active" : "inactive."),
                "setActive", DEBUG_FLAG);
        this.active = active;

    }

    public void setActiveGossip(boolean activeGossip) {
        DebugMessage.debug("Gossip is now " + (activeGossip ? "active." : "inactive."),
                "setActive", DEBUG_FLAG);
        this.activeGossip = activeGossip;
    }

    /**
     * Checks if the server is active
     *
     * @return boolean
     */
    public boolean isActive() {
        return active;
    }

    public boolean isActiveGossip() {return activeGossip; }

    /**
     * Checks if the server is a primary
     *
     * @return boolean
     */
    public boolean isPrimary() {
        return primary;
    }

    public Timestamp getValueTimestamp() {
        return valueTimestamp;
    }

    public void addReplica(String host, int port) {
        timestampLock.writeLock().lock();
        if (!replicaTimestamp.contains(host + ":" + port)) {
            replicaTimestamp.put(host + ":" + port, 0);
            valueTimestamp.put(host + ":" + port, 0);
            Timestamp newTimestamp = new Timestamp();
            newTimestamp.put(host + ":" + port, 0);
            tableTimestamp.keySet().forEach(sa -> {
                newTimestamp.put(sa, 0);
                tableTimestamp.get(sa).put(host + ":" + port, 0);
            });
            tableTimestamp.put(host + ":" + port, newTimestamp);
        }
        timestampLock.writeLock().unlock();
    }

    public Class getStudentClass(boolean isAdmin) throws InactiveServerException {
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "enroll", DEBUG_FLAG);
            throw new InactiveServerException();
        }
        return studentClass;
    }

    public String getHost() {
        return host;
    }

    public int getPort()  {
        return port;
    }

    public LogReport reportLogRecords() {
        /* make report of log records atomic */
        stateLock.writeLock().lock();
        LogReport result = new LogReport(log.values().stream()
                .filter(lr -> !lr.getStatus().equals(LogRecord.Status.NONE))
                .collect(Collectors.toSet()), valueTimestamp, host + ":" + port);
        stateLock.writeLock().unlock();
        return result;
    }

    /**
     * Wrapper function to be executed everytime updates come from other replica
     * @param logRecords
     * @param writeTimestamp
     * @param issuer
     * @param isAdmin
     * @throws InactiveServerException
     */
    public void integrateLogRecords(List<LogRecord> logRecords, Timestamp writeTimestamp, String issuer,
                                    boolean isAdmin) throws InactiveServerException {
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }
        /* make integration of incoming log records atomic */
        stateLock.writeLock().lock();
        mergeLogRecords(logRecords, writeTimestamp, issuer);
        applyUpdates();
        discardLogRecords();
        stateLock.writeLock().unlock();

    }

    /**
     * Merge inocming log records with the log
     **/
    public void mergeLogRecords(List<LogRecord> logRecords, Timestamp writeTimestamp, String issuer) {

        DebugMessage.debug("Current log:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), "mergeLogRecords", DEBUG_FLAG);

        DebugMessage.debug("Received + " +  writeTimestamp.toString() + ":\n" +
                logRecords.stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);

        DebugMessage.debug("Filtering records not before " + replicaTimestamp.toString(),
                null, DEBUG_FLAG);

        // sort log records in increasing timestamp order to allow incremental replicaTimestamp merge */
        List<LogRecord> orderedLogRecords = topologicalSort(logRecords);

        for (LogRecord lr : orderedLogRecords) {
            System.out.println(lr);
        }

        for (LogRecord lr: orderedLogRecords) {
            if (!replicaTimestamp.biggerThan(lr.getTimestamp())) {
                log.put(lr.getTimestamp(), lr);
                tableTimestamp.put(lr.getReplicaManagerId(), lr.getTimestamp());
            }
        }

        /* if there are no incoming logs, update table timestamp info to speed up log flushing */
        replicaTimestamp.merge(writeTimestamp);
        tableTimestamp.put(issuer, writeTimestamp);

        DebugMessage.debug("After merging:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);

        DebugMessage.debug("Replica Timestamp: " + replicaTimestamp.toString(), null, DEBUG_FLAG);
        DebugMessage.debug("Table Timestamp:\n" +
                tableTimestamp.keySet().stream().map(t -> t + " -> " + tableTimestamp.get(t).toString())
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);
    }

    /**
     * Do all applicable update operations in the log
     */
    public void applyUpdates() {

        /* sort log records according to causal order (o.getTimestamp()) */
        List<LogRecord> orderedUpdates = topologicalSort(log.values());

        System.out.println("Sorted records");
        for (LogRecord lr : orderedUpdates) {
            System.out.println(lr);
        }

        /* Sort concurrent records by physical clock */
        ArrayList<ArrayList<LogRecord>> concurrentBags = new ArrayList<>();
        for (int k = 0; k < orderedUpdates.size(); k++) {
            if (k == 0) {
                ArrayList<LogRecord> newArray = new ArrayList<>();
                newArray.add(orderedUpdates.get(k));
                concurrentBags.add(newArray);
            } else {
                if (orderedUpdates.get(k).getUpdate().getTimestamp()
                        .biggerThan(orderedUpdates.get(k - 1).getUpdate().getTimestamp()) &&
                    !orderedUpdates.get(k).getUpdate().getTimestamp()
                        .isEqual(orderedUpdates.get(k - 1).getUpdate().getTimestamp())) {
                    ArrayList<LogRecord> newArray = new ArrayList<>();
                    newArray.add(orderedUpdates.get(k));
                    concurrentBags.add(newArray);
                } else {
                    concurrentBags.get(concurrentBags.size() - 1).add(orderedUpdates.get(k));
                }
            }
        }

        for (List<LogRecord> alr : concurrentBags) {
            List<LogRecord> concurrent = alr.stream().sorted(new Comparator<LogRecord>() {
                @Override
                public int compare(LogRecord o1, LogRecord o2) {
                    return Longs.compare(o1.getPhysicalClock(), o2.getPhysicalClock());
                }
            }).toList();

            for (LogRecord lr : concurrent) {
                try {

                    /* only perform operations that have not been executed or that have been rolled back */
                    if ((!executedOperations.contains(lr.getTimestamp()) ||
                            suspendedOperations.contains(lr.getTimestamp()))) {

                        /* do not try to apply operation to state if that has not worked in other replica
                           e.g.: initially closed enrollments, enroll in one replica and openEnrollments in
                           another may cause an unexpected enrollment in the latter
                         */
                        if (lr.getStatus().equals(LogRecord.Status.FAIL)) {
                            timestampLock.writeLock().lock();
                            valueTimestamp.merge(lr.getTimestamp());
                            timestampLock.writeLock().unlock();
                            executedOperations.add(lr.getTimestamp());
                            DebugMessage.debug("Current timestamp after applying "
                                    + lr.getUpdate().getOperationName()
                                    + ": " + valueTimestamp.toString() + "with id "
                                    + lr.getTimestamp().toString(), "executeUpdate", DEBUG_FLAG);
                        } else {
                            /* Check for conflicts in successful operations coming from other replica */
                            if (lr.getStatus().equals(LogRecord.Status.SUCCESS)) {
                                checkConflicts(lr);
                            }
                            executeUpdate(lr);
                            lr.setStatus(LogRecord.Status.SUCCESS);
                        }

                    }
                } catch (ClassDomainException e) {
                    /* to guard against the case where a suspended SUCCESSFUL operation in another replica cannot be
                       applied to the current state */
                    if (lr.getStatus().equals(LogRecord.Status.NONE)) {
                        lr.setStatus(LogRecord.Status.FAIL);
                    }
                }
            }

        }

    }


    /**
     * Checks conflicts of an update of a log record with previous operations
     * @param record
     * @throws ClassDomainException
     */
    public void checkConflicts(LogRecord record) throws ClassDomainException {

        DebugMessage.debug("Checking conflicts for " + record.toString()
                , "checkConflicts", DEBUG_FLAG);

        /* increasing physical clock ordered relevant updates */
        List<LogRecord> toUndo = log.values().stream().filter(
                lr -> ((lr.getUpdate().getOperationName().equals("enroll") ||                   /* relevant operations */
                                lr.getUpdate().getOperationName().equals("closeEnrollments")) &&
                                executedOperations.contains(lr.getTimestamp()) &&               /* must have been applied */
                                lr.getStatus().equals(LogRecord.Status.SUCCESS) &&              /* in a successful manner */
                                 (lr.getPhysicalClock() > record.getPhysicalClock()) &&         /* must have occurred physically after */
                                !Objects.equals(lr.getReplicaManagerId(), record.getReplicaManagerId()))) /* in another replica */
                .sorted(new Comparator<LogRecord>() {
                    @Override
                    public int compare(LogRecord o1, LogRecord o2) {
                        return - Long.compare(o1.getPhysicalClock(), o2.getPhysicalClock());
                    }
                }).toList();

        DebugMessage.debug("Students enrolled in the meantime: \n" +
                toUndo.stream().filter(lr -> lr.getUpdate().getOperationName().equals("enroll"))
                        .map(lr -> lr.getUpdate().getOperationArgs().get(0))
                .collect(Collectors.joining("\n")), "checkConflicts", DEBUG_FLAG);

        switch (record.getUpdate().getOperationName()) {

            case "enroll":

                /* remove last successfully enrolled student */
                if (studentClass.isFullClass()) {
                    boolean found = false;
                    for (LogRecord lr : toUndo) {
                        if (lr.getUpdate().getOperationName().equals("enroll") &&
                                studentClass.isStudentEnrolled(lr.getUpdate().getOperationArgs().get(0))) {
                            executedOperations.remove(lr.getTimestamp());
                            suspendedOperations.add(lr.getTimestamp());
                            studentClass.removeEnrolledStudent(lr.getUpdate().getOperationArgs().get(0));
                            DebugMessage.debug("Removed " + lr.getUpdate().getOperationArgs().get(0)
                                    + " student's enrollment", null, DEBUG_FLAG);
                            found = true;
                            break;
                        }
                    }
                    /* if there were no conflicting enrolls found, suspend the operation to try to apply it later */
                    if (!found) {
                        suspendedOperations.add(record.getTimestamp());
                    }
                }

                /* Undo last cancelEnrollments operation */
                if (!studentClass.areRegistrationsOpen()) {
                    boolean found = false;
                    for (LogRecord lr : toUndo) {
                        if (lr.getUpdate().getOperationName().equals("closeEnrollments")) {
                            studentClass.setRegistrationsOpen(true);
                            executedOperations.remove(lr.getTimestamp());
                            suspendedOperations.add(lr.getTimestamp());
                            DebugMessage.debug("Suspend registrations closing operation ",
                                    null, DEBUG_FLAG);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        suspendedOperations.add(record.getTimestamp());
                    }
                }

                break;

            case "closeEnrollments":
                /* remove all currently enrolled students */
                for (LogRecord lr : toUndo) {
                    if (lr.getUpdate().getOperationName().equals("enroll") &&
                            studentClass.isStudentEnrolled(lr.getUpdate().getOperationArgs().get(0))) {
                        executedOperations.remove(lr.getTimestamp());
                        suspendedOperations.add(lr.getTimestamp());
                        studentClass.removeEnrolledStudent(lr.getUpdate().getOperationArgs().get(0));
                        DebugMessage.debug("Revoked " + lr.getUpdate().getOperationArgs().get(0)
                                + " student's enrollment", null, DEBUG_FLAG);
                    }
                }
                break;

            default:
                break;
        }
    }

    /**
     * Dispatches a log record to the corresponding update operation
     * @param record
     * @throws ClassDomainException
     */
    public void executeUpdate(LogRecord record) throws ClassDomainException {

        ClassDomainException caughtException = null;
        DebugMessage.debug("Executing " + record.getTimestamp().toString(),
                "executeUpdate", DEBUG_FLAG);

        List<String> arguments = record.getUpdate().getOperationArgs();
        try {
            switch (record.getUpdate().getOperationName()) {
                case "enroll" -> studentClass.enroll(new ClassStudent(arguments.get(0), arguments.get(1)));
                case "openEnrollments" -> studentClass.openEnrollments(Integer.parseInt(arguments.get(0)));
                case "closeEnrollments" -> studentClass.closeEnrollments();
                case "cancelEnrollment" -> studentClass.revokeEnrollment(arguments.get(0));
                default -> {
                }
            }
        } catch (ClassDomainException e) {
            caughtException = e;
        }

        timestampLock.writeLock().lock();
        valueTimestamp.merge(record.getTimestamp());
        timestampLock.writeLock().unlock();
        executedOperations.add(record.getTimestamp());
        DebugMessage.debug("Current timestamp after applying " + record.getUpdate().getOperationName()
                + ": " + valueTimestamp.toString() + "with id " + record.getTimestamp().toString(),
                "executeUpdate", DEBUG_FLAG);

        if (caughtException != null) {
            throw caughtException;
        } else { /* remove the 'suspended' status if a previously suspended the operation was successful */
            suspendedOperations.remove(record.getTimestamp());
        }
    }

    /**
     * Remove from the update log all the log records that have been seen everywhere
     */
    public void discardLogRecords() {

        DebugMessage.debug("Before discarding:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), "discardLogRecords", DEBUG_FLAG);
        log.keySet().forEach(ts -> {

            /* replica that issued the log record */
            String issuer = log.get(ts).getReplicaManagerId();

            /* see if all tableTimestamp entries have an entry for the issuer bigger than its own */
            if (tableTimestamp.keySet().stream()
                    .allMatch(sa -> tableTimestamp.get(sa).get(issuer) >= log.get(ts).getTimestamp().get(issuer))) {
                if (suspendedOperations.contains(ts)) {
                    commitSuspendedOperation(log.get(ts));
                }
                log.remove(ts);
            }

        });
        DebugMessage.debug("After discarding:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), "discardLogRecords", DEBUG_FLAG);
    }

    /**
     * Add student that could not be enrolled due to a write commit to the revoked student's list
     * @param record
     */
    public void commitSuspendedOperation(LogRecord record) {
        switch(record.getUpdate().getOperationName()) {
            case "enroll":

                studentClass.addRevokedStudent(new ClassStudent(record.getUpdate().getOperationArgs().get(0),
                        record.getUpdate().getOperationArgs().get(1)));
                DebugMessage.debug("Definitely Revoked " + record.getUpdate().getOperationArgs().get(0)
                        + " student's enrollment", null, DEBUG_FLAG);

            default:
                break;
        }
    }

    /**
     * Issue an update from the client and execute it if the replica contains all the updates that
     * happened-before it
     * @param u
     * @param readTimestamp
     * @param isAdmin
     * @return
     * @throws InactiveServerException
     * @throws InvalidOperationException
     * @throws ClassDomainException
     * @throws UpdateIssuedException
     */
    public Timestamp issueUpdate(StateUpdate u, Timestamp readTimestamp, boolean isAdmin)
            throws InactiveServerException, InvalidOperationException,
            ClassDomainException, UpdateIssuedException {

        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!u.getOperationName().equals("enroll") && !primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.",
                    null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        Timestamp resultTimestamp = new Timestamp(u.getTimestamp().getMap());

        /* Allow for concurrent issuing of updates (thus the read lock) */
        stateLock.readLock().lock();

        timestampLock.writeLock().lock();
        replicaTimestamp.put(host + ":" + port, replicaTimestamp.get(host + ":" + port) + 1);
        resultTimestamp.put(host + ":" + port, replicaTimestamp.get(host + ":" + port));
        timestampLock.writeLock().unlock();

        log.put(resultTimestamp, new LogRecord(host + ":" + port, resultTimestamp, u,
                Instant.now().toEpochMilli(), LogRecord.Status.NONE));
        DebugMessage.debug("Issued update:\n" + log.get(resultTimestamp).toString(),
                "issueUpdate", DEBUG_FLAG);

        /* check if issued update is stable */
        if (valueTimestamp.biggerThan(readTimestamp)) {
            try {
                executeUpdate(log.get(resultTimestamp));
                executedOperations.add(resultTimestamp);
                log.get(resultTimestamp).setStatus(LogRecord.Status.SUCCESS);
                stateLock.readLock().unlock();
                return resultTimestamp;
            } catch (ClassDomainException e) {
                log.get(resultTimestamp).setStatus(LogRecord.Status.FAIL);
                executedOperations.add(resultTimestamp);
                stateLock.readLock().unlock();
                e.setTimestamp(resultTimestamp);
                throw e;
            }
        } else {
            stateLock.readLock().unlock();
            throw new UpdateIssuedException(resultTimestamp);
        }

    }

    /**
     * Report the class state in an indivisible, coherent manner
     * @param isAdmin
     * @return
     * @throws InactiveServerException
     */
    public ClassStateReport reportClassState(boolean isAdmin) throws InactiveServerException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        /* Assure that the timestamp is not altered while the internal Class State is being fetched */
        timestampLock.readLock().lock();
        ClassStateReport report = studentClass.reportClassState();
        report.setTimestamp(this.valueTimestamp);
        timestampLock.readLock().unlock();
        DEBUG_FLAG = true;
        return report;
    }

    /**
     * Make a topological sorting of log records according to update timestamps
     * @return
     */
    public List<LogRecord> topologicalSort(Collection<LogRecord> log) {
        HashMap<LogRecord, ArrayList<LogRecord>> adjacencyGraph = new HashMap<>();
        List<LogRecord> records = new ArrayList<>(log);
        for (int i = 0; i < records.size(); i++) {
            for (int j = 0; j < records.size(); j++) {
                adjacencyGraph.put(records.get(i), new ArrayList<>());
            }
        }
        for (int i = 0; i < records.size(); i++) {
            for (int j = 0; j < records.size(); j++) {
                if (records.get(i).getUpdate().getTimestamp().strictlyBiggerThan(
                        records.get(j).getUpdate().getTimestamp())) {
                    adjacencyGraph.get(records.get(j)).add(records.get(i));
                }
            }
        }
        LinkedList<LogRecord> result = new LinkedList<>();
        HashSet<LogRecord> visited = new HashSet<>();
        Stack<LogRecord> stack = new Stack<>();


        for (LogRecord lr : records) {
            if (visited.contains(lr)) {
                continue;
            }
            stack.push(lr);
            while (!stack.isEmpty()) {
                LogRecord curr = stack.peek();
                visited.add(curr);
                System.out.println("In " + curr.toString());
                boolean found = false;
                for (LogRecord neighbor : adjacencyGraph.get(curr)) {
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result.addFirst(curr);
                    stack.pop();
                }

            }

        }

        return result;

    }
}