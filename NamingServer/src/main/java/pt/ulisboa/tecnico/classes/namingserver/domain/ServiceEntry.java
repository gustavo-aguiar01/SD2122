package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceEntry {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    Set<ServerEntry> serverEntries;
    String serviceName;

    public ServiceEntry(String serviceName) {
        this.serverEntries = new HashSet<ServerEntry>();
        this.serviceName = serviceName;
    }

    public void addServer(ServerEntry serverEntry) {
        serverEntries.add(serverEntry);
    }

    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
