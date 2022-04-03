package pt.ulisboa.tecnico.classes.namingserver.domain;

import java.util.List;

public class ServerEntry {

    private final String host;
    private final int port;
    // TODO: list of qualifiers
    private final boolean primary;

    public ServerEntry(String host, int port, List<String> qualifiers) {
        this.host = host;
        this.port = port;
        this.primary = qualifiers.get(0).equals("P");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
