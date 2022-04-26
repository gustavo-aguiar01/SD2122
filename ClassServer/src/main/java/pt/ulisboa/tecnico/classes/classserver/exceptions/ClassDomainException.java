package pt.ulisboa.tecnico.classes.classserver.exceptions;

import pt.ulisboa.tecnico.classes.Timestamp;

public class ClassDomainException extends Exception {

    Timestamp timestamp;

    public ClassDomainException() {}

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}