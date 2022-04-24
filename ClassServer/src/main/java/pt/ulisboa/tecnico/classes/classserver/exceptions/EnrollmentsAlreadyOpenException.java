package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class EnrollmentsAlreadyOpenException extends Exception {

    public EnrollmentsAlreadyOpenException() {}

    public String getMessage() {
        return "Enrollments are already open";
    }
}
