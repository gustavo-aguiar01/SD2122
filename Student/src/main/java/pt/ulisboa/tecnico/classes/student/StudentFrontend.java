package pt.ulisboa.tecnico.classes.student;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.*;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;

import java.util.*;

public class StudentFrontend extends Frontend {

    private final ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub;
    private final ManagedChannel namingServerChannel;

    public String serviceName;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public StudentFrontend(String hostname, int port, String serviceName) {
        this.namingServerChannel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        this.namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        this.serviceName = serviceName;

        try {
            List<Qualifier> emptyQualifiers = new ArrayList<>();
            List<Qualifier> writeQualifiers = new ArrayList<>();
            writeQualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("P").build());
            setServers(super.allServers, emptyQualifiers);
            setServers(super.writeServers, writeQualifiers);
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
        servers.addAll(responseServers);
    }

    /**
     * "enroll" client remote call facade
     * @param id
     * @param name
     * @return String
     * @throws RuntimeException
     */
    public String enroll(String id, String name) throws RuntimeException {

        DebugMessage.debug("Calling remote call enroll", "enroll", DEBUG_FLAG);
        Student newStudent = Student.newBuilder().setStudentId(id).setStudentName(name).build();
        EnrollRequest request = EnrollRequest.newBuilder().setStudent(newStudent).build();
        EnrollResponse response;

        try {
            response = (EnrollResponse) exchangeMessages(request,
                    StudentServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    StudentServiceGrpc.StudentServiceBlockingStub.class.getMethod("enroll", EnrollRequest.class),
                    x -> (((EnrollResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                DebugMessage.debug("Invalid arguments passed", null, DEBUG_FLAG);
                return e.getStatus().getDescription();
            } else {
                DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
                throw new RuntimeException(e.getStatus().getDescription());

            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * "list" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String listClass() throws RuntimeException {

        try {

            DebugMessage.debug("Calling remote call listClass", "listClass", DEBUG_FLAG);

            ListClassResponse response = (ListClassResponse) exchangeMessages(ListClassRequest.getDefaultInstance(),
                    StudentServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    StudentServiceGrpc.StudentServiceBlockingStub.class.getMethod("listClass", ListClassRequest.class),
                    x -> (((ListClassResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), false);

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
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        namingServerChannel.shutdown();
    }

}
