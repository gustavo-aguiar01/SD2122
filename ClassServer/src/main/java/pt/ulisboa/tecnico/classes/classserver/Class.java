package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class Class {

    int capacity;
    boolean openRegistrations = false;
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

    public boolean isOpenRegistrations() {
        return openRegistrations;
    }

    public void setOpenRegistrations(boolean openRegistrations) {
        this.openRegistrations = openRegistrations;
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


    public boolean contains(String studentId) {
        return enrolledStudents.containsKey(studentId);
    }

    public void enroll(ClassStudent student) {
        enrolledStudents.put(student.getId(), student);
        debug("Enrolled student with id: " + student.getId() + " and name: " + student.getName());
    }

    public ClassesDefinitions.ResponseCode openEnrollments(int capacity) {
        if (openRegistrations == true) {
            return ClassesDefinitions.ResponseCode.ENROLLMENTS_ALREADY_OPENED;
        }
        setCapacity(capacity);
        setOpenRegistrations(true);
        return ClassesDefinitions.ResponseCode.OK;
    }

    public ClassesDefinitions.ResponseCode closeEnrollments() {
        if (openRegistrations == false) {
            return ClassesDefinitions.ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
        }
        setCapacity(0); // optional ; whenever we want to open enrollments again capacity must be an argument
        setOpenRegistrations(false);
        return ClassesDefinitions.ResponseCode.OK;
    }

    public ClassesDefinitions.ResponseCode cancelEnrollment(String id) {
        if (enrolledStudents.get(id) == null) {
            return ClassesDefinitions.ResponseCode.NON_EXISTING_STUDENT;
        }
        enrolledStudents.remove(id);
        return ClassesDefinitions.ResponseCode.OK;
    }

}
