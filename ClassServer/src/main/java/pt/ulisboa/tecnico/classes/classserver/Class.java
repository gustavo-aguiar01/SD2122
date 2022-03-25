package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class Class {

    int capacity;

    boolean registrationsOpen = false;
    private ConcurrentHashMap<String, ClassStudent> enrolledStudents = new ConcurrentHashMap<String, ClassStudent>();
    private ConcurrentHashMap<String, ClassStudent> revokedStudents = new ConcurrentHashMap<String, ClassStudent>();

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

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

    public synchronized void enroll(ClassStudent student) throws EnrollmentsAlreadyClosedException, StudentAlreadyEnrolledException, FullClassException  {

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " + (openRegistrations ? "open" : "closed"), "enroll", DEBUG_FLAG);
        if (!openRegistrations) {
            throw new EnrollmentsAlreadyClosedException();
        }

        boolean studentEnrolled = this.isStudentEnrolled(student.getId());
        DebugMessage.debug("Student is " + (studentEnrolled ? "" : "not") + " enrolled", null, DEBUG_FLAG);
        if (studentEnrolled) {
            throw new StudentAlreadyEnrolledException();
        }

        boolean fullClass = this.isFullClass();
        DebugMessage.debug("Class is " + (!fullClass ? "" : "not") + " full, capacity = " + enrolledStudents.size(), null, DEBUG_FLAG);
        if (fullClass) {
            throw new FullClassException();
        }

        enrolledStudents.put(student.getId(), student);
        DebugMessage.debug("Enrolled student with id: " + student.getId() + " and name: " + student.getName(), null, DEBUG_FLAG);

    }

    public synchronized void openEnrollments(int capacity) throws EnrollmentsAlreadyOpenException, FullClassException {

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " + (openRegistrations ? "open" : "closed"), "openEnrollments", DEBUG_FLAG);
        if (openRegistrations) {
            throw new EnrollmentsAlreadyOpenException();
        }

        DebugMessage.debug("Class is " + (registrationsOpen ? "" : "not") + " full, capacity = " + enrolledStudents.size(), null, DEBUG_FLAG);
        if (isFullClass()) {
            throw new FullClassException();
        }

        setCapacity(capacity);
        setRegistrationsOpen(true);
        DebugMessage.debug("Opened class enrollment registrations with capacity of " + capacity + "!", null, DEBUG_FLAG);
    }

    public synchronized void closeEnrollments() throws EnrollmentsAlreadyClosedException {

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " + (openRegistrations ? "open" : "closed"), "closeEnrollments", DEBUG_FLAG);
        if (! openRegistrations) {
            throw new EnrollmentsAlreadyClosedException();
        }

        setRegistrationsOpen(false);
        DebugMessage.debug("Closed class enrollment registrations!", null, DEBUG_FLAG);
    }

    public synchronized void revokeEnrollment(String id) throws NonExistingStudentException {

        boolean studentEnroled = this.isStudentEnrolled(id);
        DebugMessage.debug("Student with id = " + id + " is " + (studentEnroled ? "" : "not") + " enrolled in this class", "revokeEnrollment", DEBUG_FLAG);
        if (! this.isStudentEnrolled(id)) {
            throw new NonExistingStudentException();
        }

        revokedStudents.put(id, enrolledStudents.get(id));
        enrolledStudents.remove(id);
        DebugMessage.debug("Revoked student " + id + "'s registration from class!", null, DEBUG_FLAG);
    }

}
