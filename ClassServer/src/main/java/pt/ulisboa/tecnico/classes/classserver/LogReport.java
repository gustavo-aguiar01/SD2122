package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.Timestamp;

import java.util.Collection;

public class LogReport {
    Collection<LogRecord> logRecords;
    Timestamp timestamp;
    String issuer;

    public LogReport(Collection<LogRecord> logRecords, Timestamp timestamp, String issuer) {
        this.logRecords = logRecords;
        this.timestamp = timestamp;
        this.issuer = issuer;
    }

    public Collection<LogRecord> getLogRecords() {
        return logRecords;
    }

    public void setLogRecords(Collection<LogRecord> logRecords) {
        this.logRecords = logRecords;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
