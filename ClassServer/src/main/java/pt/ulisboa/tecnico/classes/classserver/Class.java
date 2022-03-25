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

    /**
     * Getter for class capacity
     * @return int
     */
    public synchronized int getCapacity() {
        return capacity;
    }

    /**
     * Setter for class capacity
     * @param capacity
     */
    public synchronized void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Check if registrations are open
     * @return boolean
     */
    public synchronized boolean areRegistrationsOpen() {
        return registrationsOpen;
    }

    /**
     * Set registrations state to openRegistrations
     * @param openRegistrations
     */
    public synchronized void setRegistrationsOpen(boolean openRegistrations) {
        this.registrationsOpen = openRegistrations;
    }

    /**
     * Getter of the collection of all enrolled students
     * @return Collection<ClassStudent>
     */
    public Collection<ClassStudent> getEnrolledStudentsCollection() {
        return this.enrolledStudents.values();
    }

    /**
     * Getter for the collection of all enrolled students
     * @return ConcurrentHashMap<String, ClassStudent>
     */
    public ConcurrentHashMap<String, ClassStudent> getEnrolledStudents() {
        return enrolledStudents;
    }

    /**
     * Setter for the collection of all enrolled students
     * @param enrolledStudents
     */
    public void setEnrolledStudents(ConcurrentHashMap<String, ClassStudent> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    /**
     * Getter for the collection of all revoked students
     * @return Collection<ClassStudent>
     */
    public Collection<ClassStudent> getRevokedStudentsCollection() {
        return this.revokedStudents.values();
    }

    /**
     * Getter for the collection of all revoked students
     * @return ConcurrentHashMap<String, ClassStudent>
     */
    public ConcurrentHashMap<String, ClassStudent> getRevokedStudents() {
        return revokedStudents;
    }

    /**
     * Setter for the collection of all revoked students
     * @param revokedStudents
     */
    public void setRevokedStudents(ConcurrentHashMap<String, ClassStudent> revokedStudents) {
        this.revokedStudents = revokedStudents;
    }

    /**
     * Check if a student with a given ID is enrolled in this class
     * @param studentId
     * @return boolean
     */
    public boolean isStudentEnrolled(String studentId) {
        return enrolledStudents.containsKey(studentId);
    }

    /**
     * Check if the class is full
     * @return boolean
     */
    public synchronized boolean isFullClass() {
        return enrolledStudents.size() >= capacity;
    }

    /**
     * Enroll a student in this class
     * @param student
     * @throws EnrollmentsAlreadyClosedException
     * @throws StudentAlreadyEnrolledException
     * @throws FullClassException
     */
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

    /**
     * Open class for student enrollments
     * @param capacity
     * @throws EnrollmentsAlreadyOpenException
     * @throws FullClassException
     */
    public synchronized void openEnrollments(int capacity) throws EnrollmentsAlreadyOpenException, FullClassException {

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " + (openRegistrations ? "open" : "closed"), "openEnrollments", DEBUG_FLAG);
        if (openRegistrations) {
            throw new EnrollmentsAlreadyOpenException();
        }

        boolean fullClass = this.getEnrolledStudentsCollection().size() >= capacity;
        DebugMessage.debug("Class is " + (registrationsOpen ? "" : "not") + " full, capacity = " + enrolledStudents.size(), null, DEBUG_FLAG);
        if (fullClass) {
            throw new FullClassException();
        }

        setCapacity(capacity);
        setRegistrationsOpen(true);
        DebugMessage.debug("Opened class enrollment registrations with capacity of " + capacity + "!", null, DEBUG_FLAG);
    }

    /**
     * Close class for student enrollments
     * @throws EnrollmentsAlreadyClosedException
     */
    public synchronized void closeEnrollments() throws EnrollmentsAlreadyClosedException {

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " + (openRegistrations ? "open" : "closed"), "closeEnrollments", DEBUG_FLAG);
        if (! openRegistrations) {
            throw new EnrollmentsAlreadyClosedException();
        }

        setRegistrationsOpen(false);
        DebugMessage.debug("Closed class enrollment registrations!", null, DEBUG_FLAG);
    }

    /**
     * Revoke a student enrollment
     * @param id
     * @throws NonExistingStudentException
     */
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
