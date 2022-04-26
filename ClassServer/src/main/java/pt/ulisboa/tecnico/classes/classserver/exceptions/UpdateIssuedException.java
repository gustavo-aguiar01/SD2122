package pt.ulisboa.tecnico.classes.classserver.exceptions;

import pt.ulisboa.tecnico.classes.Timestamp;

public class UpdateIssuedException extends Exception {

    Timestamp timestamp;

    public UpdateIssuedException(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
