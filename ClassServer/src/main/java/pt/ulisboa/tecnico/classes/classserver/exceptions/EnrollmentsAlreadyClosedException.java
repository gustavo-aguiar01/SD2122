package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class EnrollmentsAlreadyClosedException extends ClassDomainException {

    public EnrollmentsAlreadyClosedException() {}

    public String getMessage() {
        return "Enrollments are already closed";
    }
}
