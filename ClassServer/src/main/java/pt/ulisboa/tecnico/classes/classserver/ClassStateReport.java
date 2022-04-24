package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.classserver.domain.ClassStudent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents a snapshot of a Class
 */
public class ClassStateReport {

    private final int capacity;
    private final boolean areRegistrationsOpen;
    private final Collection<ClassStudent> enrolledStudents = new ArrayList<>();
    private final Collection<ClassStudent> revokedStudents = new ArrayList<>();
    private final Map<String, Integer> timestamp = new HashMap<>();

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

    public Map<String, Integer> getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Map<String, Integer> timestamp) {
        this.timestamp.putAll(timestamp);
    }

}
