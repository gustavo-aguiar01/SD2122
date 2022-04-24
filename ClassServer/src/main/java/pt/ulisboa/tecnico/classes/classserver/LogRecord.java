package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

import java.util.Map;

public class LogRecord {

    private String replicaManagerId;
    private Timestamp timestamp;
    private StateUpdate update;

    public LogRecord(String replicaManagerId, Timestamp timestamp, StateUpdate update) {
        this.replicaManagerId = replicaManagerId;
        this.timestamp = timestamp;
        this.update = update;
    }

    public String getReplicaManagerId() {
        return replicaManagerId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public StateUpdate getUpdate() {
        return update;
    }

    public String toString() {
        return "< ReplicaId: " + replicaManagerId + ", timestamp: " + timestamp.toString()
                + ", Update: " + update.toString();
    }
}
