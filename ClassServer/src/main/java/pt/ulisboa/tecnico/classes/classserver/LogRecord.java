package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

import java.util.Map;

public class LogRecord {

    private String replicaManagerId;
    private Timestamp timestamp;
    private StateUpdate update;
    private boolean applied;

    public LogRecord(String replicaManagerId, Timestamp timestamp, StateUpdate update) {
        this.replicaManagerId = replicaManagerId;
        this.timestamp = timestamp;
        this.update = update;
        this.applied = false;
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

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }
}
