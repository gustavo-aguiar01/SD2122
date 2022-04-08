package pt.ulisboa.tecnico.classes.classserver.domain;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class that represents a snapshot of a Class
 */
public class ClassStateReport {

    private final int capacity;
    private final boolean areRegistrationsOpen;
    private final Collection<ClassStudent> enrolledStudents = new ArrayList<>();
    private final Collection<ClassStudent> revokedStudents = new ArrayList<>();
    private final int versionNumber;

    public ClassStateReport(int capacity, boolean areRegistrationsOpen,
                            Collection<ClassStudent> enrolledStudents, Collection<ClassStudent> revokedStudents,
                            int versionNumber ) {

        this.capacity = capacity;
        this.areRegistrationsOpen = areRegistrationsOpen;
        this.enrolledStudents.addAll(enrolledStudents.stream().map(ClassStudent::copyClassStudent).toList());
        this.revokedStudents.addAll(revokedStudents.stream().map(ClassStudent::copyClassStudent).toList());
        this.versionNumber = versionNumber;

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

    public int getVersionNumber() { return versionNumber; }
}
