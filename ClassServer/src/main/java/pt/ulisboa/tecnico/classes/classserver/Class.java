package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class Class {

    int capacity;

    boolean registrationsOpen = false;
    private ConcurrentHashMap<String, ClassStudent> enrolledStudents = new ConcurrentHashMap<String, ClassStudent>();
    private ConcurrentHashMap<String, ClassStudent> revokedStudents = new ConcurrentHashMap<String, ClassStudent>();

    /** Set flag to true to print debug messages.
     * The flag can be set using the -Ddebug command line option. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public Class() {}

    public synchronized int getCapacity() {
        return capacity;
    }

    public synchronized void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public synchronized boolean areRegistrationsOpen() {
        return registrationsOpen;
    }

    public synchronized void setRegistrationsOpen(boolean openRegistrations) {
        this.registrationsOpen = openRegistrations;
    }

    public Collection<ClassStudent> getEnrolledStudentsCollection() {
        return this.enrolledStudents.values();
    }

    public ConcurrentHashMap<String, ClassStudent> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(ConcurrentHashMap<String, ClassStudent> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public Collection<ClassStudent> getRevokedStudentsCollection() {
        return this.revokedStudents.values();
    }

    public ConcurrentHashMap<String, ClassStudent> getRevokedStudents() {
        return revokedStudents;
    }

    public void setRevokedStudents(ConcurrentHashMap<String, ClassStudent> revokedStudents) {
        this.revokedStudents = revokedStudents;
    }

    public boolean isStudentEnrolled(String studentId) {
        return enrolledStudents.containsKey(studentId);
    }

    public synchronized boolean isFullClass() {
        return enrolledStudents.size() >= capacity;
    }

    public synchronized void enroll(ClassStudent student) {

        debug("Registrations are " + (registrationsOpen ? "open" : "closed") + " and there are " +
                (enrolledStudents.size()) + " enrolled students in a class with capacity " + capacity);

        if (registrationsOpen == true && isFullClass() == false) {
            enrolledStudents.put(student.getId(), student);
            debug("Enrolled student with id: " + student.getId() + " and name: " + student.getName());
        }
    }

    public void openEnrollments(int capacity) {
        setCapacity(capacity);
        setRegistrationsOpen(true);
        debug("Opened class enrollment registrations with capacity of " + capacity + "!");
    }

    public void closeEnrollments() {
        setRegistrationsOpen(false);
        debug("Closed class enrollment registrations!");
    }

    public void revokeEnrollment(String id) {
        revokedStudents.put(id, enrolledStudents.get(id));
        enrolledStudents.remove(id);
        debug("Revoked student " + id + "'s registration from class!");
    }

}
