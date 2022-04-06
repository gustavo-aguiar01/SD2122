package pt.ulisboa.tecnico.classes.student;

import io.grpc.*;

import pt.ulisboa.tecnico.classes.ClientFrontend;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentFrontend extends ClientFrontend {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public StudentFrontend(String hostname, int port, String serviceName) {
        super(hostname, port, serviceName);
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

        DebugMessage.debug("Calling remote call listClass", "listClass", DEBUG_FLAG);
        ListClassRequest request = ListClassRequest.getDefaultInstance();
        ListClassResponse response;

        try {
            response = (ListClassResponse) exchangeMessages(request,
                    StudentServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    StudentServiceGrpc.StudentServiceBlockingStub.class.getMethod("listClass", ListClassRequest.class),
                    x -> (((ListClassResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), false);

            ResponseCode code = response.getCode();
            String message = Stringify.format(code);
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);

            if (response.getCode() != ResponseCode.OK) {
                return message;
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
        super.shutdown();
    }

}
