package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;

import java.util.concurrent.ConcurrentHashMap;

public class Class {

    int capacity;
    boolean openRegistrations = false;
    ConcurrentHashMap<String, Student> enrolledStudents = new ConcurrentHashMap<String, Student>();
    ConcurrentHashMap<String, Student> revokedStudents = new ConcurrentHashMap<String, Student>();

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

    public ConcurrentHashMap<String, Student> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(ConcurrentHashMap<String, Student> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public ConcurrentHashMap<String, Student> getRevokedStudents() {
        return revokedStudents;
    }

    public void setRevokedStudents(ConcurrentHashMap<String, Student> revokedStudents) {
        this.revokedStudents = revokedStudents;
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
