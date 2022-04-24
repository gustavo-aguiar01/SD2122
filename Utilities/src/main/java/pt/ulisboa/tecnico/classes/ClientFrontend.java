package pt.ulisboa.tecnico.classes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;
import com.google.protobuf.GeneratedMessageV3;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ClientFrontend {

    // Set flag to true to print debug messages
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    // writeServers - every write server for service @param "serviceName"
    // readServers - every read server for service @param "serviceName"
    // allServers - writeServers u readServers
    protected final ArrayDeque<ServerAddress> allServers = new ArrayDeque<>();
    protected final ArrayDeque<ServerAddress> primaryServers = new ArrayDeque<>();
    protected final ArrayDeque<ServerAddress> secondaryServers = new ArrayDeque<>();

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    public final String serviceName;

    public final int deadlineSecs = 5;

    protected Map<String, Integer> timestamp = new HashMap<>();

    public ClientFrontend(String hostname, int port, String serviceName) throws RuntimeException {
        this.namingServerChannel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        this.namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        this.serviceName = serviceName;
        refreshServers(); // This can throw RuntimeException to be handled in all clients.
    }

    /**
     * Initialize queue's of service servers
     * @param servers
     * @param qualifiers
     * @throws StatusRuntimeException
     */
    public void setServers(Queue<ServerAddress> servers, List<Qualifier> qualifiers) throws StatusRuntimeException {
        List<ServerAddress> responseServers = namingServerStub.lookup(LookupRequest.newBuilder()
                .setServiceName(serviceName)
                .addAllQualifiers(qualifiers).build()).getServersList();
        servers.clear();
        servers.addAll(responseServers);
    }

    /**
     * Refresh servers' queues according to all existing qualifiers.
     */
    public void refreshServers() throws RuntimeException {
        List<Qualifier> emptyQualifiers = new ArrayList<>();
        List<Qualifier> writeQualifiers = new ArrayList<>();
        List<Qualifier> readQualifiers = new ArrayList<>();
        writeQualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("P").build());
        readQualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("S").build());
        setServers(allServers, emptyQualifiers);
        if (allServers.size() == 0) {
            throw new RuntimeException("No servers available for service " + serviceName + "!");
        }
        setServers(primaryServers, writeQualifiers);
        setServers(secondaryServers, readQualifiers);

        allServers.stream().forEach(sa -> timestamp.put(sa.getHost() + ":" + sa.getPort(), 0));

        DebugMessage.debug("Current timestamp:\n" +
                        timestamp.keySet().stream().map(q ->  q + " : " + timestamp.get(q) + "\n")
                                .collect(Collectors.joining()), "refreshServers", DEBUG_FLAG);

    }

    /**
     * generic function to exchange messages with a cluster
     * of servers that implement a given service 
     * @param message
     * @param stubCreator
     * @param stubMethod
     * @param continueCondition
     * @param isWrite
     * @return GeneratedMessageV3
     */
    public GeneratedMessageV3 exchangeMessages(GeneratedMessageV3 message,  Method stubCreator, Method stubMethod,
                                               Function<GeneratedMessageV3, Boolean> continueCondition, boolean isWrite) {

        GeneratedMessageV3 response = null;

        for (int i = 0; i < (isWrite ? primaryServers.size() : allServers.size()); i++) {

            // Choose a server according to type of operation
            ServerAddress sa = isWrite ? primaryServers.peek() : allServers.peek();

            // Put at end of queue
            allServers.remove(sa);
            allServers.addLast(sa);

            // If by chance a primary server was chosen even if a read operation was requested, adjust the writing servers queue as well
            if (primaryServers.contains(sa)) {
                primaryServers.remove(sa);
                primaryServers.addLast(sa);
            }

            DebugMessage.debug("Trying server @ " + sa.getHost() + ":" + sa.getPort() + "...",
                    "exchangeMessages", DEBUG_FLAG);

            ManagedChannel channel = null;

            try {
                channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
                AbstractBlockingStub stub = (AbstractBlockingStub) stubCreator.invoke(null, channel);

                response = (GeneratedMessageV3) stubMethod.invoke(stub.withDeadlineAfter(deadlineSecs, TimeUnit.SECONDS), message);

                // If we have a valid response or if we've iterated through every available server for the requested operation return message
                if (!continueCondition.apply(response) || i == (isWrite ? primaryServers.size() : allServers.size()) - 1) {
                    channel.shutdown();
                    return response;
                }

            } catch (InvocationTargetException ite) {
                StatusRuntimeException e = (StatusRuntimeException) ite.getTargetException();
                if (!(e.getStatus().getCode() == Status.Code.UNAVAILABLE && i < (isWrite ? primaryServers.size() : allServers.size()) - 1)) {
                    if (channel != null) { channel.shutdown(); }
                    throw e;
                }

            } catch (IllegalAccessException | IllegalArgumentException iae) {
                if (channel != null) { channel.shutdown(); }
                throw new RuntimeException(iae.getMessage());
            }
        }

        return response;
    }

    public void mergeTimestamp(Map<String, Integer> newTimestamp) {
        newTimestamp.keySet().stream().forEach(sa -> timestamp.put(sa, newTimestamp.get(sa)));
        DebugMessage.debug("Current timestamp:\n" +
                DebugMessage.timestampToString(timestamp), "mergeTimestamp", DEBUG_FLAG);
    }

    public void shutdown() { namingServerChannel.shutdown(); }

}
