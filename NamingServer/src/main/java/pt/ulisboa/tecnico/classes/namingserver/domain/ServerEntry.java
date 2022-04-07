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

    /**
     * getter for hostname
     * @return String
     */
    public String getHost() {
        return host;
    }

    /**
     * getter for port
     * @return int
     */
    public int getPort() {
        return port;
    }

    /**
     * getter for this server qualifiers
     * @return Map<String, String>
     */
    public Map<String, String> getQualifiers() { return qualifiers; }

    /**
     * getter for a specific qualifier value
     * @param qualifierName
     * @return String
     */
    public String getQualifierValue(String qualifierName) {
        return qualifiers.get(qualifierName);
    }

    /**
     * string representation of a server entry
     *      host:port
     * @return String
     */
    @Override
    public String toString() {
        return host + ":" + port;
    }
}
