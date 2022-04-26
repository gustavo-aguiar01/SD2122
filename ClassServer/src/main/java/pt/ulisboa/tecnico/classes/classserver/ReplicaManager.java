package pt.ulisboa.tecnico.classes.classserver;

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
    private Set<Timestamp> executedOperations = ConcurrentHashMap.newKeySet();
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
        DebugMessage.debug("Server is now " + (active ? "active" : "inactive."), "setActive", DEBUG_FLAG);
        this.active = active;

    }

    public void setActiveGossip(boolean activeGossip) {
        DebugMessage.debug("Gossip is now " + (activeGossip ? "active." : "inactive."), "setActive", DEBUG_FLAG);
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
            DebugMessage.debug("It's not possible to obtain student class.", "enroll", DEBUG_FLAG);
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

    public void integrateLogRecords(List<LogRecord> logRecords, Timestamp writeTimestamp, String issuer,
                                    boolean isAdmin) throws InactiveServerException {
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }
        /* make integration of incoming log records atomic */
        stateLock.writeLock().lock();
        mergeLogRecords(logRecords, writeTimestamp, issuer);
        applyUpdates();
        discardLogRecords();
        stateLock.writeLock().unlock();

    }
    public void mergeLogRecords(List<LogRecord> logRecords, Timestamp writeTimestamp, String issuer) {

        DebugMessage.debug("Current log:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), "mergeLogRecords", DEBUG_FLAG);

        DebugMessage.debug("Received + " +  writeTimestamp.toString() + ":\n" + logRecords.stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);

        DebugMessage.debug("Filtering those which are not before " + replicaTimestamp.toString(),
                null, DEBUG_FLAG);

        List<LogRecord> orderedLogRecords = logRecords.stream().sorted(new Comparator<LogRecord>() {
            @Override
            public int compare(LogRecord o1, LogRecord o2) {
                if (o1.getTimestamp().biggerThan(o2.getTimestamp())) {
                    return 1;
                } else if (o2.getTimestamp().biggerThan(o1.getTimestamp())) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }).toList();

        for (LogRecord lr: orderedLogRecords) {
            System.out.println("Seeing " + lr.toString());
            if (!replicaTimestamp.biggerThan(lr.getTimestamp())) {
                log.put(lr.getTimestamp(), lr);
                replicaTimestamp.merge(lr.getTimestamp());
                tableTimestamp.put(lr.getReplicaManagerId(), lr.getTimestamp());
            }
        }


        /* if there are no incoming logs, update table timestamp info */
        tableTimestamp.put(issuer, writeTimestamp);

        DebugMessage.debug("After merging:\n" + log.values().stream().map(LogRecord::toString)
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);

        DebugMessage.debug("Replica Timestamp: " + replicaTimestamp.toString(), null, DEBUG_FLAG);
        DebugMessage.debug("Table Timestamp:\n" + tableTimestamp.keySet().stream().map(t -> t + " -> " + tableTimestamp.get(t).toString())
                .collect(Collectors.joining("\n")), null, DEBUG_FLAG);
    }

    public void applyUpdates() {

        List<LogRecord> orderedUpdates = log.values().stream().sorted(new Comparator<LogRecord>() {
            @Override
            public int compare(LogRecord o1, LogRecord o2) {
                if (o1.getUpdate().getTimestamp().biggerThan(o2.getUpdate().getTimestamp())) {
                    return 1;
                } else if (o2.getUpdate().getTimestamp().biggerThan(o1.getUpdate().getTimestamp())) {
                    return -1;
                } else {
                   return Long.compare(o1.getPhysicalClock(), o2.getPhysicalClock());
                }
            }
        }).toList();

        for (LogRecord lr : orderedUpdates) {
            try {
                /* only do operations that have not been executed or that have been rolled back */
                if ((!executedOperations.contains(lr.getTimestamp()) ||
                     suspendedOperations.contains(lr.getTimestamp()))) {
                    if (lr.getStatus().equals(LogRecord.Status.SUCCESS)) {
                        checkConflicts(lr);
                    }
                    executeUpdate(lr);
                }
                lr.setStatus(LogRecord.Status.SUCCESS);
            } catch (ClassDomainException e) {
                if (lr.getStatus().equals(LogRecord.Status.NONE)) {
                    lr.setStatus(LogRecord.Status.FAIL);;
                }
            }
        }
    }

    public void checkConflicts(LogRecord record) throws ClassDomainException {

        DebugMessage.debug("Checking conflicts for " + record.toString()
                , "checkConflicts", DEBUG_FLAG);

        /* get students that were successfully enrolled in a different replica in the meantime */
        List<LogRecord> toUndo = log.values().stream().filter(
                        lr -> ((lr.getUpdate().getOperationName().equals("enroll") ||
                                lr.getUpdate().getOperationName().equals("closeEnrollments")) &&
                                executedOperations.contains(lr.getTimestamp()) &&
                                lr.getStatus().equals(LogRecord.Status.SUCCESS) &&
                                (lr.getPhysicalClock() > record.getPhysicalClock()) &&
                                !Objects.equals(lr.getReplicaManagerId(), record.getReplicaManagerId())))
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

                if (!studentClass.areRegistrationsOpen()) {
                    boolean found = false;
                    for (LogRecord lr : toUndo) {
                        if (lr.getUpdate().getOperationName().equals("cancelEnrollments")) {
                            executedOperations.remove(lr.getTimestamp());
                            suspendedOperations.add(lr.getTimestamp());
                            DebugMessage.debug("Suspend registrations closing operation ",
                                    null, DEBUG_FLAG);
                            found = true;
                            break;
                        }
                    }
                    /* if there were no conflicting enrolls found, suspend the operation to try to apply it later */
                    if (!found) {
                        suspendedOperations.add(record.getTimestamp());
                    }
                }

                break;

            case "closeEnrollments":
                /* remove all currently enrolled students */
                for (LogRecord lr : toUndo) {
                    if (studentClass.isStudentEnrolled(lr.getUpdate().getOperationArgs().get(0))) {
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

    public void executeUpdate(LogRecord record) throws ClassDomainException {

        ClassDomainException caughtException = null;
        DebugMessage.debug("Executing " + record.getTimestamp().toString(), "executeUpdate", DEBUG_FLAG);

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
                + ": " + valueTimestamp.toString() + "with id " + record.getTimestamp().toString(), "executeUpdate", DEBUG_FLAG);

        if (caughtException != null) {
            throw caughtException;
        } else { /* remove the 'suspended' status if a previously suspended the operation was successful */
            suspendedOperations.remove(record.getTimestamp());
        }
    }

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

    public Timestamp issueUpdate(StateUpdate u, boolean isAdmin) throws InactiveServerException,
            InvalidOperationException, ClassDomainException, UpdateIssuedException {

        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!u.getOperationName().equals("enroll") && !primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.",
                    null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        Timestamp resultTimestamp = new Timestamp(u.getTimestamp().getMap());

        /* Allow for concurrent issuing of updates */
        stateLock.readLock().lock();

        timestampLock.writeLock().lock();
        replicaTimestamp.put(host + ":" + port, replicaTimestamp.get(host + ":" + port) + 1);
        resultTimestamp.put(host + ":" + port, replicaTimestamp.get(host + ":" + port));
        timestampLock.writeLock().unlock();

        log.put(resultTimestamp, new LogRecord(host + ":" + port, resultTimestamp, u,
                Instant.now().toEpochMilli(), LogRecord.Status.NONE));
        DebugMessage.debug("Issued update:\n" + log.get(resultTimestamp).toString(), "issueUpdate", DEBUG_FLAG);

        /* check if issued update is stable */
        if (valueTimestamp.biggerThan(u.getTimestamp())) {
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

    public ClassStateReport reportClassState(boolean isAdmin) throws InactiveServerException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "reportClassState", DEBUG_FLAG);
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
}