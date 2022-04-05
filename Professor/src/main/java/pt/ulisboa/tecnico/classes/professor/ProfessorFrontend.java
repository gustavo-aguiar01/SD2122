package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfessorFrontend {

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    private final List<ClassServerNamingServer.ServerAddress> primaryServers;
    private final List<ClassServerNamingServer.ServerAddress> allServers;

    private ProfessorServiceGrpc.ProfessorServiceBlockingStub stub;
    private ManagedChannel channel;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);


    public ProfessorFrontend(String hostname, int port, String serviceName) {
        this.namingServerChannel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        this.namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        List<Qualifier> emptyQualifiers = new ArrayList<Qualifier>();
        this.allServers = namingServerStub.lookup(LookupRequest.newBuilder()
                .setServiceName(serviceName)
                .addAllQualifiers(emptyQualifiers).build()).getServersList();

        List<Qualifier> primaryQualifier = new ArrayList<Qualifier>();
        primaryQualifier.add(Qualifier.newBuilder().setName("primaryStatus").setValue("P").build());
        this.primaryServers = namingServerStub.lookup(LookupRequest.newBuilder()
                .setServiceName(serviceName)
                .addAllQualifiers(primaryQualifier).build()).getServersList();
    }

    private void setWritingServer () {
        if (this.primaryServers.size() == 0) {
            // TODO : return error code? or throw error
            return;
        }

        ServerAddress server = primaryServers.get(0);
        System.out.printf(server.getHost());
        System.out.printf(" " + Integer.toString(server.getPort()) + "\n");
        channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort()).usePlaintext().build();
        stub = ProfessorServiceGrpc.newBlockingStub(channel);
    }

    private void setReadingServer () {
        if (this.allServers.size() == 0) {
            // TODO : return error code? or throw error
            return;
        }

        ServerAddress server = allServers.get(0);
        System.out.printf(server.getHost());
        System.out.printf(" " + Integer.toString(server.getPort()) + "\n");
        channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort()).usePlaintext().build();
    }


    /**
     * "openEnrollments" client remote call facade
     * @param capacity
     * @return String
     * @throws RuntimeException
     */
    public String openEnrollments(int capacity) throws RuntimeException {
        setWritingServer();

        try {
            DebugMessage.debug("Calling remote call openEnrollments", "openEnrollments", DEBUG_FLAG);
            ProfessorClassServer.OpenEnrollmentsResponse responseOpenEnrollments = stub.openEnrollments(
                    ProfessorClassServer.OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build());

            String message = Stringify.format(responseOpenEnrollments.getCode());
            DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
            return message;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                DebugMessage.debug("Invalid arguments passed", null, DEBUG_FLAG);
                return e.getStatus().getDescription();
            } else {
                DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
                throw new RuntimeException(e.getStatus().getDescription());
            }
        }
    }

    /**
     * "closeEnrollments" client remote call facade
     * @return String
     */
    public String closeEnrollments() {
        setWritingServer();

        try {
            DebugMessage.debug("Calling remote call closeEnrollments", "closeEnrollments", DEBUG_FLAG);
            ProfessorClassServer.CloseEnrollmentsResponse responseCloseEnrollments = stub.closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest.getDefaultInstance());

            String message = Stringify.format(responseCloseEnrollments.getCode());
            DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
            return message;
        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    /**
     * "list" client remote call facade
     * @return String
     */
    public String listClass() {
        setReadingServer();

        try {
            DebugMessage.debug("Calling remote call listClass", "listClass", DEBUG_FLAG);
            ProfessorClassServer.ListClassResponse response = stub.listClass(ProfessorClassServer.ListClassRequest.getDefaultInstance());

            String message = Stringify.format(response.getCode());
            DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
            if (response.getCode() != ClassesDefinitions.ResponseCode.OK) {
                return Stringify.format(response.getCode());
            }

            DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);
            return Stringify.format(response.getClassState());
        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    /**
     * "cancelEnrollments" client remote call facade
     * @param id
     * @return
     * @throws RuntimeException
     */
    public String cancelEnrollment(String id) throws RuntimeException {
        setWritingServer();

        try {
            DebugMessage.debug("Calling remote call cancelEnrollment", "cancelEnrollment", DEBUG_FLAG);
            ProfessorClassServer.CancelEnrollmentResponse responseCancelEnrollments = stub.cancelEnrollment(
                    ProfessorClassServer.CancelEnrollmentRequest.newBuilder().setStudentId(id).build());

            String message = Stringify.format(responseCancelEnrollments.getCode());
            DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
            return message;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                DebugMessage.debug("Invalid arguments passed", null, DEBUG_FLAG);
                return e.getStatus().getDescription();
            } else {
                DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
                throw new RuntimeException(e.getStatus().getDescription());
            }
        }
    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        channel.shutdown();
    }

}
