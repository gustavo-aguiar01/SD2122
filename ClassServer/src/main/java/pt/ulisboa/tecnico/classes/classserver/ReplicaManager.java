package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassStudent;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReplicaManager {

    private static boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private boolean active;
    private Class studentClass;
    private boolean primary;
    private String host;
    private int port;
    private Map<String, Integer> timestamp = new HashMap<>();
    ReentrantReadWriteLock timestampLock = new ReentrantReadWriteLock();

    public ReplicaManager (String primary, String host, int port) {
        this.studentClass = new Class();
        this.active = true;
        this.primary = primary.equals("P");
        this.studentClass = new Class();
        this.host = host;
        this.port = port;
        this.timestamp.put(host + ":" + port, 0);
    }

    /**
     * Set server availability
     * @param active
     */
    public void setActive(boolean active) {
        DebugMessage.debug("Server is now " + (active ? "active" : "inactive."), "setActive", DEBUG_FLAG);
        this.active = active;

    }

    /**
     * Checks if the server is active
     * @return boolean
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Checks if the server is a primary
     * @return boolean
     */
    public boolean isPrimary() {
        return primary;
    }

    public Map<String, Integer> getTimestamp() { return timestamp; }

    public void addReplica(String host, int port) {
        timestampLock.writeLock().lock();
        if (!timestamp.keySet().contains(host + ":" + port)) {
            timestamp.put(host + ":" + port, 0);
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

    public Map<String, Integer> enroll(ClassStudent student, boolean isAdmin) throws InactiveServerException,
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
        timestamp.put(host + ":" + port, timestamp.get(host + ":" + port) + 1);
        Map<String, Integer> res = timestamp;
        timestampLock.writeLock().unlock();

        return timestamp;

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
        report.setTimestamp(this.timestamp);
        timestampLock.readLock().unlock();
        DEBUG_FLAG = true;
        return report;
    }

    public ClassStateReport getClassState(Map<String, Integer> timestamp, boolean isAdmin) throws InactiveServerException,
            NotUpToDateException {

        if (!timestampBigger(this.timestamp, timestamp)) {
            DebugMessage.debug("Current version \n" + DebugMessage.timestampToString(this.timestamp) +
                    "is behind received request's version \n" + DebugMessage.timestampToString(timestamp), "reportClassState", DEBUG_FLAG);
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
                              Map<String, Integer> timestamp, boolean isAdmin) throws InactiveServerException {
        DEBUG_FLAG = false;
        /* Can only access server contents if the server is active */
        if (!isAdmin && !this.isActive()) {
            DebugMessage.debug("It's not possible to obtain student class.", "reportClassState", DEBUG_FLAG);
            throw new InactiveServerException();
        }

        DebugMessage.debug("Setting received class state...", "setClassState", DEBUG_FLAG);
        timestampLock.writeLock().lock();
        studentClass.setClassState(capacity, areRegistrationsOpen, enrolledStudents, revokedStudents);
        this.timestamp = timestamp;
        DebugMessage.debug("Current timestamp:\n" +
                DebugMessage.timestampToString(timestamp), "setClassState", true);
        timestampLock.writeLock().unlock();
        DEBUG_FLAG = true;

    }

    public Map<String, Integer> openEnrollments(int capacity, boolean isAdmin) throws InactiveServerException,
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
        timestamp.put(host + ":" + port, timestamp.get(host + ":" + port) + 1);
        Map<String, Integer> res = timestamp;
        timestampLock.writeLock().unlock();

        return timestamp;

    }

    public Map<String, Integer> closeEnrollments(boolean isAdmin) throws InactiveServerException,
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
        timestamp.put(host + ":" + port, timestamp.get(host + ":" + port) + 1);
        Map<String, Integer> res = timestamp;
        timestampLock.writeLock().unlock();

        return timestamp;

    }

    public Map<String, Integer> revokeEnrollment(String studentId, boolean isAdmin) throws InvalidOperationException,
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
        timestamp.put(host + ":" + port, timestamp.get(host + ":" + port) + 1);
        Map<String, Integer> res = timestamp;
        timestampLock.writeLock().unlock();

        return timestamp;
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
