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
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ClientFrontend {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private final ArrayDeque<ClassServerNamingServer.ServerAddress> allServers = new ArrayDeque<>();
    private final ArrayDeque<ClassServerNamingServer.ServerAddress> writeServers = new ArrayDeque<>();

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    public final String serviceName;

    public ClientFrontend(String hostname, int port, String serviceName) {
        this.namingServerChannel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        this.namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        this.serviceName = serviceName;

        try {
            List<ClassServerNamingServer.Qualifier> emptyQualifiers = new ArrayList<>();
            List<ClassServerNamingServer.Qualifier> writeQualifiers = new ArrayList<>();
            writeQualifiers.add(ClassServerNamingServer.Qualifier.newBuilder().setName("primaryStatus").setValue("P").build());
            setServers(allServers, emptyQualifiers);
            setServers(writeServers, writeQualifiers);
            DebugMessage.debug("Got the following servers:\n" +
                    allServers.stream().map(sa ->  sa.getHost() + " : " + sa.getPort() + "\n")
                    .collect(Collectors.joining()) + " and the following write servers\n" +
                    writeServers.stream().map(sa ->  sa.getHost() + " : " + sa.getPort() + "\n")
                            .collect(Collectors.joining()), "ClientFrontend Constructor", DEBUG_FLAG);

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
    public void setServers(Queue<ClassServerNamingServer.ServerAddress> servers, List<ClassServerNamingServer.Qualifier> qualifiers) throws StatusRuntimeException {
        List<ClassServerNamingServer.ServerAddress> responseServers = namingServerStub.lookup(ClassServerNamingServer.LookupRequest.newBuilder()
                .setServiceName(serviceName)
                .addAllQualifiers(qualifiers).build()).getServersList();
        servers.addAll(responseServers);
    }

    public GeneratedMessageV3 exchangeMessages(GeneratedMessageV3 message,  Method stubCreator, Method stubMethod,
                                               Function<GeneratedMessageV3, Boolean> continueCondition, boolean isWrite) {
        GeneratedMessageV3 response = null;

        for (int i = 0; i < (isWrite ? writeServers.size() : allServers.size()); i++) {

            /* choose a server according to type of operation */
            ClassServerNamingServer.ServerAddress sa = isWrite ? writeServers.peek() : allServers.peek();

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
