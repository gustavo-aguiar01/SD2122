package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentFrontend {

    private final StudentServiceGrpc.StudentServiceBlockingStub stub;
    private final ManagedChannel channel;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public StudentFrontend(String hostname, int port) {
        channel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    /**
     * "enroll" client remote call facade
     * @param id
     * @param name
     * @return String
     * @throws RuntimeException
     */
    public String enroll(String id, String name) throws RuntimeException {

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
