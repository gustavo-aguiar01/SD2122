package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.*;

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

    public List<ServerAddress> lookupServersOfService (String serviceName, List<String> qualifiers) {

        DebugMessage.debug("Looking up servers of service " + serviceName + "with the following qualifiers " + Arrays.toString(qualifiers.toArray()),
                "lookupServersOfService", DEBUG_FLAG);
        if (!serviceEntries.containsKey(serviceName)) {
            DebugMessage.debug("The service associated with the service name " + serviceName + " does not exist",
                    null, DEBUG_FLAG);
            return new ArrayList<ServerAddress>();
        }

        DebugMessage.debug("Filtering servers of service " + serviceName + " based on qualifiers",
                null, DEBUG_FLAG);
        return serviceEntries.get(serviceName).lookupServers(qualifiers);
    }
}
