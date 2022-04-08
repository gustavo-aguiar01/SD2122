package pt.ulisboa.tecnico.classes.namingserver.domain;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.AlreadyExistingPrimaryServerException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.AlreadyExistingServerException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.InvalidServerInfoException;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NamingServices {

    // Set flag to true to print debug messages
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private final ReentrantReadWriteLock servicesLock = new ReentrantReadWriteLock();

    private final Map<String, ServiceEntry> serviceEntries;

    public NamingServices() {
        this.serviceEntries = new HashMap<>();
    }

    public void addService(String serviceName, String host, int port, Map<String, String> qualifiers)
            throws InvalidServerInfoException, AlreadyExistingServerException, AlreadyExistingPrimaryServerException{

        servicesLock.writeLock().lock();
        DebugMessage.debug("Inserting server " + host + ":" + port + " with the following qualifiers:\n" +
                        qualifiers.keySet().stream().map(q ->  q + " : " + qualifiers.get(q) + "\n")
                                .collect(Collectors.joining()) + "to service " + serviceName + ".",
                "addService", DEBUG_FLAG);

        if (!((1024 <= port) && (port <= 65535)) ||
                !(qualifiers.containsKey("primaryStatus")) ||
                !(Arrays.asList(new String[]{"P", "S"}).contains(qualifiers.get("primaryStatus")))) {
            DebugMessage.debug("Invalid server info.", null, DEBUG_FLAG);
            servicesLock.writeLock().unlock();
            throw new InvalidServerInfoException();
        }

        if (!serviceEntries.containsKey(serviceName)) {
            DebugMessage.debug("Service does not exist. Creating service...",
                    null, DEBUG_FLAG);
            serviceEntries.put(serviceName, new ServiceEntry(serviceName));
        }

        if (serviceEntries.get(serviceName).getServerEntries().stream()
                .anyMatch(se -> se.getHost().equals(host) && se.getPort() == port)) {
            DebugMessage.debug("Server @ host:port is already registered.", null, DEBUG_FLAG);
            servicesLock.writeLock().unlock();
            throw new AlreadyExistingServerException();
        }

        if (qualifiers.get("primaryStatus").equals("P") &&
                serviceEntries.get(serviceName).getServerEntries().stream()
                .anyMatch(se -> se.getQualifierValue("primaryStatus").equals("P"))) {
            DebugMessage.debug("Primary server already registered for the given service.", null, DEBUG_FLAG);
            servicesLock.writeLock().unlock();
            throw new AlreadyExistingPrimaryServerException();
        }

        serviceEntries.get(serviceName).addServer(new ServerEntry(host, port, qualifiers));
        servicesLock.writeLock().unlock();

    }

    public List<ServerEntry> lookupServersOfService(String serviceName, Map<String, String> qualifiers) {

        servicesLock.readLock().lock();

        DebugMessage.debug("Looking up server with the following qualifiers:\n" +
                        qualifiers.keySet().stream().map(q ->  q + " : " + qualifiers.get(q) + "\n")
                                .collect(Collectors.joining()) + "to service " + serviceName + ".",
                "lookupServersOfService", DEBUG_FLAG);

        if (!serviceEntries.containsKey(serviceName)) {
            DebugMessage.debug("No service associated with the service name " + serviceName + " exists.",
                    null, DEBUG_FLAG);
            servicesLock.readLock().unlock();
            return new ArrayList<>();
        }

        DebugMessage.debug("Filtering servers of service " + serviceName + " based on given qualifiers.",
                null, DEBUG_FLAG);

        // Get all server entries whose qualifiers match the given qualifiers element by element
        List<ServerEntry> result = serviceEntries.get(serviceName).getServerEntries().stream()
                .filter(se -> qualifiers.keySet().stream()
                        .allMatch(q-> qualifiers.get(q) != null && se.getQualifierValue(q).equals(qualifiers.get(q))))
                .collect(Collectors.toList());

        servicesLock.readLock().unlock();

        return result;

    }
  
    public void deleteService(String serviceName, String host, int port) {

        servicesLock.writeLock().lock();
        DebugMessage.debug("Deleting server " + host + ":" + port + " from service " + serviceName + ".", "deleteService", DEBUG_FLAG);
        ServiceEntry serviceEntry = serviceEntries.get(serviceName);
        if (serviceEntry == null) {
            DebugMessage.debug("Service " + serviceName + " does not exist.", null, DEBUG_FLAG);
            servicesLock.writeLock().unlock();
            return;
        }
        Set<ServerEntry> serverEntries = serviceEntry.getServerEntries();
        Set<ServerEntry> entriesToRemove = serverEntries.stream()
            .filter(se -> se.getHost().equals(host) && se.getPort() == port)
            .collect(Collectors.toSet());
        serverEntries.removeAll(entriesToRemove);
        servicesLock.writeLock().unlock();

    }
}
