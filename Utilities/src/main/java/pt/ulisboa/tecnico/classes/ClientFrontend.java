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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public abstract class ClientFrontend {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    protected final ArrayDeque<ServerAddress> allServers = new ArrayDeque<>();
    protected final ArrayDeque<ServerAddress> writeServers = new ArrayDeque<>();
    protected final ArrayDeque<ServerAddress> readServers = new ArrayDeque<>();

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    public final String serviceName;

    public ClientFrontend(String hostname, int port, String serviceName) {
        this.namingServerChannel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        this.namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        this.serviceName = serviceName;

        try {
            refreshServers();
        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
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
    public void refreshServers() {
        List<Qualifier> emptyQualifiers = new ArrayList<>();
        List<Qualifier> writeQualifiers = new ArrayList<>();
        List<Qualifier> readQualifiers = new ArrayList<>();
        writeQualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("P").build());
        readQualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("S").build());
        setServers(allServers, emptyQualifiers);
        setServers(writeServers, writeQualifiers);
        setServers(readServers, readQualifiers);
    }

    public GeneratedMessageV3 exchangeMessages(GeneratedMessageV3 message,  Method stubCreator, Method stubMethod,
                                               Function<GeneratedMessageV3, Boolean> continueCondition, boolean isWrite) {
        GeneratedMessageV3 response = null;

        for (int i = 0; i < (isWrite ? writeServers.size() : allServers.size()); i++) {

            /* choose a server according to type of operation */
            ServerAddress sa = isWrite ? writeServers.peek() : allServers.peek();

            allServers.remove(sa);
            allServers.addLast(sa);

            /* if by chance a Primary server was chosen even if a Read was requested, adjust the Write queue as well */
            if (writeServers.contains(sa)) {
                writeServers.remove(sa);
                writeServers.addLast(sa);
            }

            DebugMessage.debug("Trying server @" + sa.getHost() + " : " + sa.getPort(),
                    "exchangeMessages", DEBUG_FLAG);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();

            try {
                AbstractBlockingStub stub = (AbstractBlockingStub) stubCreator.invoke(null, channel);

                response = (GeneratedMessageV3) stubMethod.invoke(stub, message);

                if (!continueCondition.apply(response) || i == (isWrite ? writeServers.size() : allServers.size()) - 1) {
                    channel.shutdown();
                    return response;
                }

            } catch (InvocationTargetException ite) {
                StatusRuntimeException e = (StatusRuntimeException) ite.getTargetException();
                if (!(e.getStatus().getCode() == Status.Code.UNAVAILABLE && i < (isWrite ? writeServers.size() : allServers.size()) - 1)) {
                    channel.shutdown();
                    throw e;
                }

            } catch (IllegalAccessException | IllegalArgumentException iae) {
                channel.shutdown();
                throw new RuntimeException(iae.getMessage());
            }
        }

        return response;
    }

    public void shutdown() { namingServerChannel.shutdown(); }

}
