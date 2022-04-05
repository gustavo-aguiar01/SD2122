package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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

    private final ClassNamingServerServiceBlockingStub namingStub;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ClassFrontend(String nameServerHostname, int nameServerPort) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext().build();
        namingStub = ClassNamingServerServiceGrpc.newBlockingStub(channel);
    }

    public void register(String serviceName, String host, int port, String primary) throws RuntimeException {

        try {
            List<Qualifier> qualifiers = new ArrayList<>();
            qualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue(primary).build());

            DebugMessage.debug("Calling register remote call", "register", DEBUG_FLAG);
            namingStub.register(RegisterRequest.newBuilder().setServiceName(serviceName)
                        .setHost(host).setPort(port).addAllQualifiers(qualifiers).build());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public void delete(String serviceName, String host, int port) throws RuntimeException {

        try {
            DebugMessage.debug("Calling delete remote call", "delete", DEBUG_FLAG);
            namingStub.delete(DeleteRequest.newBuilder().setServiceName(serviceName)
                    .setHost(host).setPort(port).build());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public String propagateState(Class studentClass) throws RuntimeException {
        DebugMessage.debug("Calling propagateState remote call", "propagateState", DEBUG_FLAG);

        List<Qualifier> qualifiers = new ArrayList<>();
        qualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("S").build());

        List<ServerAddress> servers;

        try {
            servers = namingStub.lookup(LookupRequest.newBuilder()
                    .setServiceName("Turmas").addAllQualifiers(qualifiers).build()).getServersList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        if (servers.size() == 0) {
            DebugMessage.debug("No secondary servers available!", null, DEBUG_FLAG);
        }

        String message = Stringify.format(ResponseCode.OK);
        for (ServerAddress se : servers) {
            DebugMessage.debug("Propagating to secondary server @ " + se.getHost() + ":" + se.getPort(),
                                null, DEBUG_FLAG);

            ManagedChannel serverChannel = ManagedChannelBuilder.forAddress(se.getHost(), se.getPort())
                                            .usePlaintext().build();
            ClassServerServiceBlockingStub serverStub = ClassServerServiceGrpc.newBlockingStub(serverChannel);

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudentsCollection()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudentsCollection()))
                    .build();

            PropagateStateResponse response;
            try {
                response = serverStub.propagateState(PropagateStateRequest.newBuilder()
                        .setClassState(state).build());
            } catch (StatusRuntimeException e) {
                throw new RuntimeException(e.getStatus().getDescription());
            }

            ResponseCode code = response.getCode();
            message = Stringify.format(code);
            DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
            if (code == ResponseCode.OK) {
                DebugMessage.debug("Primary server propagated successfully", null, DEBUG_FLAG);
            } else {
                DebugMessage.debug("Primary server failed to propagate with code: " + code, null, DEBUG_FLAG);
            }
        }
        return message;
    }
}
