package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

public class LogRecord {

    private String replicaManagerId;
    private Timestamp timestamp;        /* unique timestamp associated with the log */
    private StateUpdate update;         /* the operation */
    private long physicalClock;         /* physical clock of server commit (to untie concurrent operations) */
    private Status status;              /* update operation on the state */

    public enum Status {
        SUCCESS,
        FAIL,
        NONE
    }

    public LogRecord(String replicaManagerId, Timestamp timestamp, StateUpdate update, long physicalClcok,
                     LogRecord.Status status) {
        this.replicaManagerId = replicaManagerId;
        this.timestamp = timestamp;
        this.update = update;
        this.physicalClock = physicalClcok;
        this.status = status;
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

    public long getPhysicalClock() { return physicalClock; }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return this.status;
    }

    public String statusToString() {
        String res;
        switch(status){
            case SUCCESS:
                res = "success";
                break;
            case FAIL:
                res = "fail";
                break;
            case NONE:
                res =  "none";
                break;
            default:
                res =  "";
                break;
        }
        return res;
    }

    public String toString() {
        return "< ReplicaId: " + replicaManagerId + ", timestamp: " + timestamp.toString()
                + ", Update: " + update.toString() + ", Physical Clock: " + Long.toString(physicalClock) + " " +
                " Successful: " + statusToString() + ">";
    }
}
