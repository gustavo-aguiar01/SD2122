package pt.ulisboa.tecnico.classes.student;

import io.grpc.*;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class StudentFrontend {

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    private StudentServiceGrpc.StudentServiceBlockingStub stub;
    private ManagedChannel channel;

    private final List<ServerAddress> primaryServers;
    private final List<ServerAddress> allServers;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public StudentFrontend(String hostname, int port, String serviceName) {
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
        this.channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort()).usePlaintext().build();
        this.stub = StudentServiceGrpc.newBlockingStub(this.channel);
    }

    private void setReadingServer () {
        if (this.allServers.size() == 0) {
            // TODO : return error code? or throw error
            return;
        }

        ServerAddress server = allServers.get(0);
        System.out.printf(server.getHost());
        System.out.printf(" " + Integer.toString(server.getPort()) + "\n");
        this.channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort()).usePlaintext().build();
        this.stub = StudentServiceGrpc.newBlockingStub(this.channel);
    }

    /**
     * "enroll" client remote call facade
     * @param id
     * @param name
     * @return String
     * @throws RuntimeException
     */
    public String enroll(String id, String name) throws RuntimeException {
        setWritingServer();
        try {

            DebugMessage.debug("Calling remote call enroll", "enroll", DEBUG_FLAG);
            Student newStudent = Student.newBuilder().setStudentId(id).setStudentName(name).build();

            String message = Stringify.format(stub.enroll(EnrollRequest.newBuilder().setStudent(newStudent).build()).getCode());
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);

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
     * "list" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String listClass() throws RuntimeException {
        setReadingServer();
        try {

            DebugMessage.debug("Calling remote call listClass", "listClass", DEBUG_FLAG);
            ListClassResponse response = stub.listClass(ListClassRequest.getDefaultInstance());

            ResponseCode code = response.getCode();
            String message = Stringify.format(code);
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);

            if (response.getCode() != ResponseCode.OK) {
                return Stringify.format(code);
            } else {
                DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);
                return Stringify.format(response.getClassState());
            }
        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        channel.shutdown();
    }

}
