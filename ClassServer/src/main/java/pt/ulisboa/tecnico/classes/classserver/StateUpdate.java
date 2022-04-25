package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

import java.util.List;
import java.util.stream.Collectors;

public class StateUpdate {
    private String operationName;
    private List<String> operationArgs;
    private Timestamp timestamp;

    public StateUpdate(String operationName, List<String> operationArgs, Timestamp timestamp) {
        this.operationName = operationName;
        this.operationArgs = operationArgs;
        this.timestamp = timestamp;
    }

    public String getOperationName() {
        return operationName;
    }

    public List<String> getOperationArgs() {
        return operationArgs;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "< Name:" + operationName + ", Arguments: " + String.join(", ", operationArgs) +
                ", Timestamp: " + timestamp + ">";
    }
}
