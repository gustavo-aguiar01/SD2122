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

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public Class() {}

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean areRegistrationsOpen() {
        return registrationsOpen;
    }

    public void setRegistrationsOpen(boolean openRegistrations) {
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

    public void enroll(ClassStudent student) {
        enrolledStudents.put(student.getId(), student);
        debug("Enrolled student with id: " + student.getId() + " and name: " + student.getName());
    }

    public void openEnrollments(int capacity) {
        setCapacity(capacity);
        setRegistrationsOpen(true);
    }

    public void closeEnrollments() {
        setCapacity(0); // optional ; whenever we want to open enrollments again capacity must be an argument
        setRegistrationsOpen(false);
    }

    public void revokeEnrollment(String id) {
        revokedStudents.put(id, enrolledStudents.get(id));
        enrolledStudents.remove(id);
    }

}
