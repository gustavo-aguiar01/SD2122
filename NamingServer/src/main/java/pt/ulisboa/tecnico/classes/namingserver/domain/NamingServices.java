package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class NamingServices {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private Map<String, ServiceEntry> serviceEntries;

    public NamingServices() {
        this.serviceEntries = new HashMap<String, ServiceEntry>();
    }

    public NamingServices(Map<String, ServiceEntry> serviceEntries) {
        this.serviceEntries = serviceEntries;
    }

    public void addService(String serviceName, String host, int port, List<String> qualifiers) {

        DebugMessage.debug("Inserting service " + serviceName + " from server " + host + ":" + port,
                "addService", DEBUG_FLAG);

        if (!serviceEntries.containsKey(serviceName)) {
            DebugMessage.debug("Service does not exist. Creating service...",
                    null, DEBUG_FLAG);
            serviceEntries.put(serviceName, new ServiceEntry(serviceName));
        }
        serviceEntries.get(serviceName).addServer(host, port, qualifiers);

    }

    public void deleteService(String serviceName, String host, int port) {
        DebugMessage.debug("Deleting server " + host + ":" + port + " from service " + serviceName, "deleteService", DEBUG_FLAG);
        ServiceEntry serviceEntry = serviceEntries.get(serviceName);
        if (serviceEntry == null) {
            DebugMessage.debug("Service " + serviceName + " does not exist!", null, DEBUG_FLAG);
            return;
        }
        Set<ServerEntry> serverEntries = serviceEntry.getServerEntries();
        Set<ServerEntry> entriesToRemove = serverEntries.stream()
            .filter(se -> se.getHost().equals(host) && se.getPort() == port)
            .collect(Collectors.toSet());
        serverEntries.removeAll(entriesToRemove);
    }
}
