package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class ClassFrontend {

    private final ClassServerServiceGrpc.ClassServerServiceBlockingStub stub;
    private final ManagedChannel channel;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ClassFrontend(String hostname, int port) {
        channel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        stub = ClassServerServiceGrpc.newBlockingStub(channel);
    }

    public void register(String serviceName, String host, int port, String primary) {

        List<String> qualifiers = new ArrayList<>();
        qualifiers.add(primary);

        DebugMessage.debug("Calling register remote call", "register", DEBUG_FLAG);
        stub.register(RegisterRequest.newBuilder().setServiceName(serviceName)
                    .setHost(host).setPort(port).addAllQualifiers(qualifiers).build());
    }

    public void delete(String serviceName, String host, int port) {
        DebugMessage.debug("Calling delete remote call", "delete", DEBUG_FLAG);
        stub.delete(DeleteRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).build());
    }
}
