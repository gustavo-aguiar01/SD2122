package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.stream.Collectors;

public class Class {

    int capacity;

    boolean registrationsOpen = false;
    private ConcurrentHashMap<String, ClassStudent> enrolledStudents = new ConcurrentHashMap<String, ClassStudent>();
    private ConcurrentHashMap<String, ClassStudent> revokedStudents = new ConcurrentHashMap<String, ClassStudent>();

    /* Set flag to true to print debug messages. */
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

    public synchronized ClassState getClassState() {
        // Construct ClassState
        List<Student> enrolledStudents = this.getEnrolledStudentsCollection().stream()
                .map(s -> Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        List<Student> discardedStudents = this.getRevokedStudentsCollection().stream()
                .map(s -> Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        ClassState state = ClassState.newBuilder().setCapacity(this.getCapacity())
                .setOpenEnrollments(this.areRegistrationsOpen())
                .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();

        return state;
    }

    public synchronized void enroll(ClassStudent student) throws EnrollmentsAlreadyClosedException, StudentAlreadyEnrolledException, FullClassException  {

        // realiability verification
        if (! this.areRegistrationsOpen()) {
            throw new EnrollmentsAlreadyClosedException();
        }

        if (this.isStudentEnrolled(student.getId())) {
            throw new StudentAlreadyEnrolledException();
        }

        if (this.isFullClass()) {
            throw new FullClassException();
        }

        debug("Registrations are " + (registrationsOpen ? "open" : "closed") + " and there are " +
                (enrolledStudents.size()) + " enrolled students in a class with capacity " + capacity);

        if (registrationsOpen == true && isFullClass() == false) {
            enrolledStudents.put(student.getId(), student);
            debug("Enrolled student with id: " + student.getId() + " and name: " + student.getName());
        }
    }

    public void openEnrollments(int capacity) throws EnrollmentsAlreadyOpenException, FullClassException {

        // realiability verification
        if (this.areRegistrationsOpen()) {
            throw new EnrollmentsAlreadyOpenException();
        }

        if (this.getEnrolledStudentsCollection().size() >= capacity) {
            throw new FullClassException();
        }

        setCapacity(capacity);
        setRegistrationsOpen(true);
        debug("Opened class enrollment registrations with capacity of " + capacity + "!");
    }

    public void closeEnrollments() throws EnrollmentsAlreadyClosedException {

        // realiability verification
        if (! this.areRegistrationsOpen()) {
            throw new EnrollmentsAlreadyClosedException();
        }

        setRegistrationsOpen(false);
        debug("Closed class enrollment registrations!");
    }

    public void revokeEnrollment(String id) throws NonExistingStudentException {

        // realiability verification
        if (! this.isStudentEnrolled(id)) {
            throw new NonExistingStudentException();
        }

        revokedStudents.put(id, enrolledStudents.get(id));
        enrolledStudents.remove(id);
        debug("Revoked student " + id + "'s registration from class!");
    }

}
