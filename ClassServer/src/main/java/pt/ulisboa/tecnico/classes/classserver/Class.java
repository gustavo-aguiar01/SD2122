package pt.ulisboa.tecnico.classes.classserver;

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

}
