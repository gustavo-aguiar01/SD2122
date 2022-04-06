package pt.ulisboa.tecnico.classes.student;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;
import com.google.protobuf.GeneratedMessageV3;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer;

import java.util.ArrayDeque;
import java.util.function.Function;

public abstract class Frontend {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    protected final ArrayDeque<ClassServerNamingServer.ServerAddress> allServers = new ArrayDeque<>();
    protected final ArrayDeque<ClassServerNamingServer.ServerAddress> writeServers = new ArrayDeque<>();

    public Frontend() {}

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

            ManagedChannel channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
            try {
                AbstractBlockingStub stub = (AbstractBlockingStub) stubCreator.invoke(null, channel);

                DebugMessage.debug("Trying server @" + sa.getHost() + " : " + sa.getPort(),
                        "exchangeMessages", DEBUG_FLAG);

                response = (GeneratedMessageV3) stubMethod.invoke(stub, message);

                if (continueCondition.apply(response) || i == writeServers.size() - 1) {
                    channel.shutdown();
                    return response;
                }

            } catch (InvocationTargetException ite) {
                StatusRuntimeException e = (StatusRuntimeException) ite.getTargetException();
                if (!(continueCondition.apply(response) && i < writeServers.size() - 1)) {
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

}
