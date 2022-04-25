package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class StudentAlreadyEnrolledException extends Exception {

    public StudentAlreadyEnrolledException() {}

    public String getMessage() {
        return "Student is already enrolled";
    }
}
