package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class EnrollmentsAlreadyOpenException extends ClassDomainException {

    public EnrollmentsAlreadyOpenException() {}

    public String getMessage() {
        return "Enrollments are already open";
    }
}
