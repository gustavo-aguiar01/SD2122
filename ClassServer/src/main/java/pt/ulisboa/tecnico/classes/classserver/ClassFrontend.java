package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;

import java.util.ArrayList;
import java.util.List;

public class ClassFrontend {

    private final ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel channel;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ClassFrontend(String nameServerHostname, int nameServerPort) {
        channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext().build();
        namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(channel);
    }

    public void register(String serviceName, String host, int port, String primary) throws RuntimeException {

        DebugMessage.debug("Calling register remote call", "register", DEBUG_FLAG);
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

        DebugMessage.debug("Calling delete remote call", "delete", DEBUG_FLAG);
        DeleteRequest request = DeleteRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).build();

        try {
            namingServerStub.delete(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public String propagateState(ClassStateReport studentClass) throws RuntimeException {

        DebugMessage.debug("Calling propagateState remote call", "propagateState", DEBUG_FLAG);
        // Propagate state to secondary servers
        List<Qualifier> qualifiers = new ArrayList<>();
        Qualifier secondary = Qualifier.newBuilder().setName("primaryStatus").setValue("S").build();
        qualifiers.add(secondary);
        LookupRequest lookupRequest = LookupRequest.newBuilder()
                .setServiceName("Turmas").addAllQualifiers(qualifiers).build();
        List<ServerAddress> servers;

        try {
            // Lookup secondary servers
            servers = namingServerStub.lookup(lookupRequest).getServersList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        String message;
        if (servers.size() == 0) {
            DebugMessage.debug("No secondary servers available!", null, DEBUG_FLAG);
            message = Stringify.format(ResponseCode.INACTIVE_SERVER);
        } else {
            message = Stringify.format(ResponseCode.OK); // This will be changed if something went wrong
        }
        for (ServerAddress se : servers) {

            DebugMessage.debug("Propagating to secondary server @ " + se.getHost() + ":" + se.getPort(),
                    null, DEBUG_FLAG);
            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudents()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudents()))
                    .build();
            PropagateStateRequest request = PropagateStateRequest.newBuilder().setClassState(state).build();
            PropagateStateResponse response;

            try {
                ManagedChannel serverChannel = ManagedChannelBuilder.forAddress(se.getHost(), se.getPort())
                        .usePlaintext().build();
                ClassServerServiceBlockingStub serverStub = ClassServerServiceGrpc.newBlockingStub(serverChannel);
                response = serverStub.propagateState(request);

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
                serverChannel.shutdown();

            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available!", null, DEBUG_FLAG);
                    return Stringify.format(ResponseCode.INACTIVE_SERVER); // Edge case where backup server closed after primary checked if servers size != 0
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
