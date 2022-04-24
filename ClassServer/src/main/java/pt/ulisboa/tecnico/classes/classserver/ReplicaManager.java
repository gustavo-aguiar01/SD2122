package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassStudent;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReplicaManager {

    private static boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private boolean active;
    private boolean activeGossip;
    private Class studentClass;
    private boolean primary;
    private String host;
    private int port;
    private Timestamp valueTimestamp = new Timestamp();
    private Timestamp replicaTimestamp = new Timestamp();
    private Map<Timestamp, LogRecord> log = new HashMap<>();

    ReentrantReadWriteLock timestampLock = new ReentrantReadWriteLock();

    public ReplicaManager (String primary, String host, int port) {
        this.studentClass = new Class();
        this.active = true;
        this.activeGossip = true;
        this.primary = primary.equals("P");
        this.studentClass = new Class();
        this.host = host;
        this.port = port;
        this.valueTimestamp.put(host + ":" + port, 0);
        this.replicaTimestamp.put(host + ":" + port, 0);
    }

    /**
     * Set server availability
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
     * @return boolean
     */
    public boolean isActive() {
        return active;
    }

    public boolean isActiveGossip() {return activeGossip; }

    /**
     * Checks if the server is a primary
     * @return boolean
     */
    public boolean isPrimary() {
        return primary;
    }

    public Timestamp getValueTimestamp() { return valueTimestamp; }

    public void addReplica(String host, int port) {
        timestampLock.writeLock().lock();
        if (!valueTimestamp.contains(host + ":" + port)) {
            valueTimestamp.put(host + ":" + port, 0);
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

    public Timestamp issueUpdate(StateUpdate u) {

        Timestamp resultTimestamp = new Timestamp(u.getTimestamp().getMap());

        timestampLock.readLock().lock();
        replicaTimestamp.put(host + ":" + port, valueTimestamp.get(host + ":" + port) + 1);
        u.getTimestamp().put(host + ":" + port, valueTimestamp.get(host + ":" + port));
        timestampLock.readLock().unlock();

        log.put(resultTimestamp, new LogRecord(host + ":" + port, resultTimestamp, u));

        return resultTimestamp;
    }

    public Timestamp enroll(ClassStudent student, boolean isAdmin) throws InactiveServerException,
            StudentAlreadyEnrolledException, EnrollmentsAlreadyClosedException,
            FullClassException, InvalidOperationException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "enroll", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.", null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        studentClass.enroll(student);

        timestampLock.writeLock().lock();
        valueTimestamp.put(host + ":" + port, valueTimestamp.get(host + ":" + port) + 1);
        Timestamp res = valueTimestamp;
        timestampLock.writeLock().unlock();

        return res;

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

    public ClassStateReport getClassState(Timestamp timestamp, boolean isAdmin) throws InactiveServerException,
            NotUpToDateException {

        if (!this.valueTimestamp.biggerThan(timestamp)) {
            DebugMessage.debug("Current version \n" +  timestamp.toString() +
                    "is behind received request's version \n" + timestamp.toString(), "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

       return reportClassState(isAdmin);
    }

    /**
     * Set the class state in an atomic manner
     * @param capacity
     * @param areRegistrationsOpen
     * @param enrolledStudents
     * @param revokedStudents
     */
    public void setClassState(int capacity, boolean areRegistrationsOpen,
                              Collection<ClassStudent> enrolledStudents, Collection<ClassStudent> revokedStudents,
                             Timestamp timestamp, boolean isAdmin) throws InactiveServerException {
        DEBUG_FLAG = false;
        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        DebugMessage.debug("Setting received class state...", "setClassState", DEBUG_FLAG);
        timestampLock.writeLock().lock();
        studentClass.setClassState(capacity, areRegistrationsOpen, enrolledStudents, revokedStudents);
        this.valueTimestamp = timestamp;
        DebugMessage.debug("Current timestamp:\n" +
                timestamp.toString(), "setClassState", true);
        timestampLock.writeLock().unlock();
        DEBUG_FLAG = true;

    }

    public Timestamp openEnrollments(int capacity, boolean isAdmin) throws InactiveServerException,
            InvalidOperationException, EnrollmentsAlreadyOpenException, FullClassException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "openEnrollments", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.",
                    null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        studentClass.openEnrollments(capacity);

        timestampLock.writeLock().lock();
        valueTimestamp.put(host + ":" + port, valueTimestamp.get(host + ":" + port) + 1);
        Timestamp res = valueTimestamp;
        timestampLock.writeLock().unlock();

        return valueTimestamp;

    }

    public Timestamp closeEnrollments(boolean isAdmin) throws InactiveServerException,
            InvalidOperationException, EnrollmentsAlreadyClosedException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "openEnrollments", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.",
                    null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        studentClass.closeEnrollments();

        timestampLock.writeLock().lock();
        valueTimestamp.put(host + ":" + port, valueTimestamp.get(host + ":" + port) + 1);
        Timestamp res = valueTimestamp;
        timestampLock.writeLock().unlock();

        return valueTimestamp;

    }

    public Timestamp revokeEnrollment(String studentId, boolean isAdmin) throws InvalidOperationException,
            InactiveServerException, NonExistingStudentException {

        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.",
                    "openEnrollments", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        if (!primary) {
            DebugMessage.debug("Cannot execute write operation on backup server.",
                    null, DEBUG_FLAG);
            throw new InvalidOperationException();
        }

        studentClass.revokeEnrollment(studentId);

        timestampLock.writeLock().lock();
        valueTimestamp.put(host + ":" + port, valueTimestamp.get(host + ":" + port) + 1);
        Timestamp res = valueTimestamp;
        timestampLock.writeLock().unlock();

        return valueTimestamp;
    }

    public boolean isUpdateApplicable(StateUpdate u) {
        return valueTimestamp.biggerThan(u.getTimestamp());
    }


    /**
     * See if timestamp ts1[i] => ts2[i], ignoring null entries of ts2 that are not present in ts1
     * @param ts1
     * @param ts2
     * @return
     */
    public boolean timestampBigger(Map<String, Integer> ts1, Map<String, Integer> ts2) {
        return ts2.keySet().stream().allMatch(sa -> (!ts1.containsKey(sa) && ts2.get(sa) == 0) ||
                (ts1.containsKey(sa) && (ts1.get(sa) >= ts2.get(sa))));
    }

}
