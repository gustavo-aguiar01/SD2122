package pt.ulisboa.tecnico.classes.classserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;


import java.util.HashMap;
import java.util.Collection;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Class {

    int capacity;
    boolean registrationsOpen = false;
    private HashMap<String, ClassStudent> enrolledStudents = new HashMap<String, ClassStudent>();
    private HashMap<String, ClassStudent> revokedStudents = new HashMap<String, ClassStudent>();

    /* To tackle the synchronization problem where a student enrolls and a professor decreases capacity */
    ReentrantReadWriteLock stateConsistencyLock = new ReentrantReadWriteLock();

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public Class() {}

    /**
     * Getter for class capacity
     * @return int
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Setter for class capacity
     * @param capacity
     */
    private void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Check if registrations are open
     * @return boolean
     */
    public boolean areRegistrationsOpen() {
        return registrationsOpen;
    }

    /**
     * Set registrations state to openRegistrations
     * @param openRegistrations
     */
    private void setRegistrationsOpen(boolean openRegistrations) {
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
    public HashMap<String, ClassStudent> getEnrolledStudents() {
        return enrolledStudents;
    }

    private void setEnrolledStudents(Collection<ClassStudent> students) {
        this.enrolledStudents.clear();
        students.stream().forEach(s -> this.enrolledStudents.put(s.getId(), s));
    }

    private void setDiscardedStudents(Collection<ClassStudent> students) {
        this.revokedStudents.clear();
        students.stream().forEach(s -> this.revokedStudents.put(s.getId(), s));
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
    public HashMap<String, ClassStudent> getRevokedStudents() {
        return revokedStudents;
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
    public boolean isFullClass() {
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

        stateConsistencyLock.writeLock().lock();
        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " +
                (openRegistrations ? "open" : "closed"), "enroll", DEBUG_FLAG);
        if (!openRegistrations) {
            stateConsistencyLock.writeLock().unlock();
            throw new EnrollmentsAlreadyClosedException();
        }

        boolean studentEnrolled = this.isStudentEnrolled(student.getId());
        DebugMessage.debug("Student is " +
                (studentEnrolled ? "" : "not") + " enrolled", null, DEBUG_FLAG);
        if (studentEnrolled) {
            stateConsistencyLock.writeLock().unlock();
            throw new StudentAlreadyEnrolledException();
        }

        boolean fullClass = this.isFullClass();
        DebugMessage.debug("Class is" +
                (fullClass ? "" : " not") + " full, capacity = " + capacity, null, DEBUG_FLAG);
        if (fullClass) {
            stateConsistencyLock.writeLock().unlock();
            throw new FullClassException();
        }

        revokedStudents.keySet().removeIf(s -> s.equals(student.getId()));
        enrolledStudents.put(student.getId(), student);
        stateConsistencyLock.writeLock().unlock();

        DebugMessage.debug("Enrolled student with id: " + student.getId()
                + " and name: " + student.getName(), null, DEBUG_FLAG);

    }


    /**
     * Open class for student enrollments
     * @param capacity
     * @throws EnrollmentsAlreadyOpenException
     * @throws FullClassException
     */
    public void openEnrollments(int capacity) throws EnrollmentsAlreadyOpenException, FullClassException {

        stateConsistencyLock.writeLock().lock();
        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " +
                (openRegistrations ? "open" : "closed"), "openEnrollments", DEBUG_FLAG);
        if (openRegistrations) {
            stateConsistencyLock.writeLock().unlock();
            throw new EnrollmentsAlreadyOpenException();
        }

        boolean fullClass = this.getEnrolledStudentsCollection().size() >= capacity;
        DebugMessage.debug("Class is " +
                (registrationsOpen ? "" : "not") + " full, capacity = " +
                enrolledStudents.size(), null, DEBUG_FLAG);
        if (fullClass) {
            stateConsistencyLock.writeLock().unlock();
            throw new FullClassException();
        }

        setCapacity(capacity);
        stateConsistencyLock.writeLock().unlock();

        setRegistrationsOpen(true);
        DebugMessage.debug("Opened class enrollment registrations with capacity of " +
                capacity + "!", null, DEBUG_FLAG);
    }

    /**
     * Close class for student enrollments
     * @throws EnrollmentsAlreadyClosedException
     */
    public void closeEnrollments() throws EnrollmentsAlreadyClosedException {

        stateConsistencyLock.writeLock().lock();

        boolean openRegistrations = this.areRegistrationsOpen();
        DebugMessage.debug("Registrations are " +
                (openRegistrations ? "open" : "closed"), "closeEnrollments", DEBUG_FLAG);
        if (!openRegistrations) {
            stateConsistencyLock.writeLock().unlock();
            throw new EnrollmentsAlreadyClosedException();
        }

        setRegistrationsOpen(false);
        stateConsistencyLock.writeLock().unlock();

        DebugMessage.debug("Closed class enrollment registrations!", null, DEBUG_FLAG);
    }

    /**
     * Revoke a student enrollment
     * @param id
     * @throws NonExistingStudentException
     */
    public synchronized void revokeEnrollment(String id) throws NonExistingStudentException {

        stateConsistencyLock.writeLock().lock();

        boolean studentEnrolled = this.isStudentEnrolled(id);
        DebugMessage.debug("Student with id = " + id + " is" +
                (studentEnrolled ? "" : " not") + " enrolled in this class", "revokeEnrollment", DEBUG_FLAG);
        if (!this.isStudentEnrolled(id)) {
            stateConsistencyLock.writeLock().unlock();
            throw new NonExistingStudentException();
        }

        revokedStudents.put(id, enrolledStudents.get(id));
        enrolledStudents.remove(id);
        stateConsistencyLock.writeLock().unlock();

        DebugMessage.debug("Revoked student " + id +
                "'s registration from class!", null, DEBUG_FLAG);
    }

    /**
     * Get a complete report of the class state in an atomic manner
     * @return the report of the class
     */
    public ClassStateReport reportClassState() {
        DebugMessage.debug("Reporting class state...  ", "reportClassState", DEBUG_FLAG);
        stateConsistencyLock.readLock().lock();
        ClassStateReport report = new ClassStateReport(capacity, areRegistrationsOpen(), getEnrolledStudentsCollection(), getRevokedStudentsCollection());
        stateConsistencyLock.readLock().unlock();
        return report;
    }

    /**
     * Set the class state in an atomic manner
     * @param capacity
     * @param areRegistrationsOpen
     * @param enrolledStudents
     * @param revokedStudents
     */
    public void setClassState(int capacity, boolean areRegistrationsOpen,
                              Collection<ClassStudent> enrolledStudents, Collection<ClassStudent> revokedStudents) {
        DebugMessage.debug("Setting received class state... ", "setClassState", DEBUG_FLAG);
        stateConsistencyLock.writeLock().lock();
        setCapacity(capacity);
        setRegistrationsOpen(areRegistrationsOpen);
        setEnrolledStudents(enrolledStudents);
        setDiscardedStudents(revokedStudents);
        stateConsistencyLock.writeLock().unlock();
    }

}