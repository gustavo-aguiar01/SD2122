package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class StudentAlreadyEnrolledException extends ClassDomainException {

    public StudentAlreadyEnrolledException() {}

    public String getMessage() {
        return "Student is already enrolled";
    }
}
