package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ClassFrontend {

    private final ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel channel;
    private final ReplicaManager replicaManager;
    private final String hostname;
    private final int port;

    // Set flag to true to print debug messages
    private static boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ClassFrontend(ReplicaManager replicaManager, String hostname, int port,
                         String nameServerHostname, int nameServerPort) {
        channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext().build();
        namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(channel);
        this.replicaManager = replicaManager;
        this.hostname = hostname;
        this.port = port;
    }

    public void register(String serviceName, String host, int port, String primary) throws RuntimeException {

        DebugMessage.debug("Calling register remote call.", "register", DEBUG_FLAG);
        List<Qualifier> qualifiers = new ArrayList<>();
        Qualifier qualifier = Qualifier.newBuilder().setName("primaryStatus").setValue(primary).build();
        qualifiers.add(qualifier);
        RegisterRequest request = RegisterRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).addAllQualifiers(qualifiers).build();

        try {
            namingServerStub.register(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

    }

    public void delete(String serviceName, String host, int port) throws RuntimeException {

        DebugMessage.debug("Calling delete remote call.", "delete", DEBUG_FLAG);
        DeleteRequest request = DeleteRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).build();

        try {
            namingServerStub.delete(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

    }

    public String propagateState(String serviceName) throws RuntimeException, InactiveServerException {
        DebugMessage.debug("Calling propagateState remote call.", "propagateState", DEBUG_FLAG);
        // check if the gossip is active
        if (!replicaManager.isActiveGossip()) {
            DebugMessage.debug("Gossip in this server is inactive", null, DEBUG_FLAG);

            return "Gossip is not active";
        }

        // Propagate state to secondary servers
        LookupRequest lookupRequest = LookupRequest.newBuilder()
                .setServiceName(serviceName).addAllQualifiers(new ArrayList<>()).build();
        List<ServerAddress> servers;

        try {
            // Lookup secondary servers
            servers = namingServerStub.withDeadlineAfter(5, TimeUnit.SECONDS).lookup(lookupRequest).getServersList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        servers.forEach(sa -> { if (!replicaManager.getValueTimestamp()
                                            .contains(sa.getHost() + ":" + sa.getPort())) {
                                             replicaManager.addReplica(sa.getHost(), sa.getPort()); }});

        String message;
        if (servers.size() == 0) {
            DebugMessage.debug("No secondary servers available.", null, DEBUG_FLAG);
            message = Stringify.format(ResponseCode.INACTIVE_SERVER);
        } else {
            message = Stringify.format(ResponseCode.OK); // This will be changed if something went wrong
        }

        //ClassStateReport studentClass = replicaManager.reportClassState(false);
        Collection<LogRecord> logRecordsCollection = replicaManager.reportLogRecords();
        Collection<ClassesDefinitions.LogRecord> logRecords = logRecordsCollection.stream()
                .map(lr -> ClassesDefinitions.LogRecord.newBuilder()
                        .setId(lr.getReplicaManagerId())
                        .putAllTimestamp(lr.getTimestamp().getMap())
                        .setUpdate(Update.newBuilder()
                                .setOperationName(lr.getUpdate().getOperationName())
                                .addAllArguments(lr.getUpdate().getOperationArgs())
                                .putAllTimestamp(lr.getUpdate().getTimestamp().getMap()).build())
                        .build())
                        .collect(Collectors.toList());

        DebugMessage.debug("Propagating log records:\n" + logRecordsCollection.stream().map(q -> q + "\n").toList(), null, DEBUG_FLAG);

        for (ServerAddress se : servers) {
            if (se.getHost().equals(hostname) && se.getPort() == port) {
                continue;
            }
            DebugMessage.debug("Propagating to secondary server @ " + se.getHost() + ":" + se.getPort() + "...",
                    null, DEBUG_FLAG);

            PropagateStateRequest request = PropagateStateRequest.newBuilder().addAllLogRecords(logRecords).build();
            PropagateStateResponse response;

            try {

                ManagedChannel serverChannel = ManagedChannelBuilder.forAddress(se.getHost(), se.getPort())
                        .usePlaintext().build();
                ClassServerServiceBlockingStub serverStub = ClassServerServiceGrpc.newBlockingStub(serverChannel);
                response = serverStub.withDeadlineAfter(5, TimeUnit.SECONDS).propagateState(request);

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                //DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
                serverChannel.shutdown();

            } catch (StatusRuntimeException e) {

                // TODO : here the gossip can be deactivated
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available.", null, DEBUG_FLAG);
                    return Stringify.format(ResponseCode.INACTIVE_SERVER); // Edge case where backup server closed after primary checked if servers size != 0
                } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                    throw new RuntimeException(e.getStatus().getDescription());
                } else {
                    // Other than that it should throw exception
                    throw new RuntimeException(e.getStatus().getDescription());
                }
            }
        }
        return message;

    }

    public void shutdown() { channel.shutdown(); }
}
