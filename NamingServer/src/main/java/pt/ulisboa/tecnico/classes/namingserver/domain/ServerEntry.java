package pt.ulisboa.tecnico.classes.namingserver.domain;

import java.util.Map;

public class ServerEntry {

    private final String host;
    private final int port;
    private Map<String, String> qualifiers;

    public ServerEntry(String host, int port, Map<String, String> qualifiers) {
        this.host = host;
        this.port = port;
        this.qualifiers = qualifiers;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getQualifiers() { return qualifiers; }

    public String getQualifierValue(String qualifierName) {
        return qualifiers.get(qualifierName);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
