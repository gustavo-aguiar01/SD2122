package pt.ulisboa.tecnico.classes.namingserver.domain;

import java.util.*;

public class ServiceEntry {

    private Set<ServerEntry> serverEntries;
    private String serviceName;

    public ServiceEntry(String serviceName) {
        this.serverEntries = new HashSet<>();
        this.serviceName = serviceName;
    }

    /**
     * add a new server entry associated with a given service
     * @param serverEntry
     */
    public void addServer(ServerEntry serverEntry) {
        serverEntries.add(serverEntry);
    }

    /**
     * getter for server entries
     * @return Set<ServerEntry>
     */
    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    /**
     * setter for server entries
     * @param serverEntries
     */
    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    /**
     * getter for service name
     * @return String
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * setter for service name
     * @param serviceName
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
