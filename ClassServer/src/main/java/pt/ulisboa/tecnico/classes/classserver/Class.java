package pt.ulisboa.tecnico.classes.classserver;

import java.util.concurrent.ConcurrentHashMap;

public class Class {

    int capacity;
    boolean openRegistrations = false;
    ConcurrentHashMap<String, classStudent> enrolledStudents = new ConcurrentHashMap<String, classStudent>();
    ConcurrentHashMap<String, classStudent> revokedStudents = new ConcurrentHashMap<String, classStudent>();

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

    public ConcurrentHashMap<String, classStudent> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(ConcurrentHashMap<String, classStudent> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public ConcurrentHashMap<String, classStudent> getRevokedStudents() {
        return revokedStudents;
    }

    public void setRevokedStudents(ConcurrentHashMap<String, classStudent> revokedStudents) {
        this.revokedStudents = revokedStudents;
    }

}
