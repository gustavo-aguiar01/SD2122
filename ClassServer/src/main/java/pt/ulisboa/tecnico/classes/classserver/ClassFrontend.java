package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.DebugMessage;
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

    public void register(String serviceName, String host, int port, String primary) throws RuntimeException {

        List<Qualifier> qualifiers = new ArrayList<Qualifier>();
        qualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue(primary).build());

        DebugMessage.debug("Calling register remote call", "register", DEBUG_FLAG);
        try {
            stub.register(RegisterRequest.newBuilder().setServiceName(serviceName)
                    .setHost(host).setPort(port).addAllQualifiers(qualifiers).build());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public void delete(String serviceName, String host, int port) {
        DebugMessage.debug("Calling delete remote call", "delete", DEBUG_FLAG);
        stub.delete(DeleteRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).build());
    }
}
