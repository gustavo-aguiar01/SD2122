package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class FullClassException extends ClassDomainException {

    public FullClassException() {}

    public String getMessage() {
        return "Class is full";
    }
}
