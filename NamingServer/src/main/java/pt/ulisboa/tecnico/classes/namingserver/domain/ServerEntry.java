package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.List;

public class ServerEntry {

    private final String host;
    private final int port;
    private List<String> qualifiers;
    private final boolean primary;

    public ServerEntry(String host, int port, List<String> qualifiers) {
        this.host = host;
        this.port = port;
        this.qualifiers = qualifiers;
        this.primary = qualifiers.get(0).equals("P");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean hasQualifier (List<String> qualifiers) {
        // TODO : does this work?
        return qualifiers.equals(this.qualifiers);
    }

    public ServerAddress proto () {
        ServerAddress serverProto = ServerAddress.newBuilder().setHost(this.host).setPort(this.port).build();
        return serverProto;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
