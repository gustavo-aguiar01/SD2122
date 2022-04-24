package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

import java.util.List;
import java.util.Map;

public class StateUpdate {
    private String operationName;
    private List<Object> operationArgs;
    private Timestamp timestamp;

    public StateUpdate(String operationName, List<Object> operationArgs, Timestamp timestamp) {
        this.operationName = operationName;
        this.operationArgs = operationArgs;
        this.timestamp = timestamp;
    }

    public String getOperationName() {
        return operationName;
    }

    public List<Object> getOperationArgs() {
        return operationArgs;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
