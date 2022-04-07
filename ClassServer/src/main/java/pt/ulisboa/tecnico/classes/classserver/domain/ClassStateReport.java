package pt.ulisboa.tecnico.classes.classserver.domain;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class that represents a snapshot of a Class
 */
public class ClassStateReport {

    private int capacity;
    private boolean areRegistrationsOpen;
    private Collection<ClassStudent> enrolledStudents = new ArrayList<>();
    private Collection<ClassStudent> revokedStudents = new ArrayList<>();

    public ClassStateReport(int capacity, boolean areRegistrationsOpen,
                            Collection<ClassStudent> enrolledStudents, Collection<ClassStudent> revokedStudents) {
        this.capacity = capacity;
        this.areRegistrationsOpen = areRegistrationsOpen;
        this.enrolledStudents.addAll(enrolledStudents.stream().map(ClassStudent::copyClassStudent).toList());
        this.revokedStudents.addAll(revokedStudents.stream().map(ClassStudent::copyClassStudent).toList());
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean areRegistrationsOpen() {
        return areRegistrationsOpen;
    }

    public Collection<ClassStudent> getEnrolledStudents() {
        return enrolledStudents;
    }

    public Collection<ClassStudent> getRevokedStudents() {
        return revokedStudents;
    }
}
