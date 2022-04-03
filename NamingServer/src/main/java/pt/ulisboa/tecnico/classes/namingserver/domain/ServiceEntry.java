package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ServiceEntry {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    Set<ServerEntry> serverEntries;
    String serviceName;

    public ServiceEntry(Set<ServerEntry> serverEntries, String serviceName) {
        this.serverEntries = serverEntries;
        this.serviceName = serviceName;
    }

    public ServiceEntry(String serviceName) {
        this.serverEntries = new HashSet<ServerEntry>();
        this.serviceName = serviceName;
    }

    // TODO: REVIEW
    public void addServer(String host, int port, List<String> qualifiers) {
        DebugMessage.debug("Inserting server " + host + ":" + port + " to service " + getServiceName(),
                "addServer", DEBUG_FLAG);
        serverEntries.add(new ServerEntry(host, port, qualifiers));
    }

    public List<ServerAddress> lookupServers (List<String> qualifiers) {

        List<ServerAddress> validServers = new ArrayList<ServerAddress>();
        // If no qualifiers are specified return all
        // servers associated with this service
        if (qualifiers.size() == 0) {
            for (ServerEntry entry : serverEntries) {
                validServers.add(entry.proto());
            }
            return validServers;
        }

        // Filter servers based on the given qualifiers
        for (ServerEntry entry : serverEntries) {
            if (entry.hasQualifier(qualifiers)) {
                validServers.add(entry.proto());
            }
        }

        return validServers;
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
