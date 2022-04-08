package pt.ulisboa.tecnico.classes.professor;

import io.grpc.*;

import pt.ulisboa.tecnico.classes.ClientFrontend;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorFrontend extends ClientFrontend {

    // Set flag to true to print debug messages.
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ProfessorFrontend(String hostname, int port, String serviceName) {
        super(hostname, port, serviceName);
    }

    /**
     * "openEnrollments" client remote call facade
     * @param capacity
     * @return String
     * @throws RuntimeException
     */
    public String openEnrollments(int capacity) throws RuntimeException {

        DebugMessage.debug("Calling remote call openEnrollments.", "openEnrollments", DEBUG_FLAG);
        OpenEnrollmentsRequest request = OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build();
        OpenEnrollmentsResponse response;

        try {

            response = (OpenEnrollmentsResponse) exchangeMessages(request,
                    ProfessorServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    ProfessorServiceGrpc.ProfessorServiceBlockingStub.class.getMethod("openEnrollments", OpenEnrollmentsRequest.class),
                    x -> (((OpenEnrollmentsResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) { // Invalid input capacity.
                DebugMessage.debug("Invalid arguments passed.", null, DEBUG_FLAG);
                return e.getStatus().getDescription();
            } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                throw new RuntimeException(e.getStatus().getDescription());
            } else {
                // Other than that it should throw exception
                throw new RuntimeException(e.getStatus().getDescription());
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * "closeEnrollments" client remote call facade
     * @return String
     */
    public String closeEnrollments() {

        DebugMessage.debug("Calling remote call closeEnrollments.", "closeEnrollments", DEBUG_FLAG);
        CloseEnrollmentsRequest request = CloseEnrollmentsRequest.getDefaultInstance();
        CloseEnrollmentsResponse response;

        try {

            response = (CloseEnrollmentsResponse) exchangeMessages(request,
                    ProfessorServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    ProfessorServiceGrpc.ProfessorServiceBlockingStub.class.getMethod("closeEnrollments", CloseEnrollmentsRequest.class),
                    x -> (((CloseEnrollmentsResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);

        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught: " + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * "list" client remote call facade
     * @return String
     */
    public String listClass() {

        DebugMessage.debug("Calling remote call listClass.", "listClass", DEBUG_FLAG);
        ListClassRequest request = ListClassRequest.getDefaultInstance();
        ListClassResponse response;

        try {

            response = (ListClassResponse) exchangeMessages(request,
                    ProfessorServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    ProfessorServiceGrpc.ProfessorServiceBlockingStub.class.getMethod("listClass", ListClassRequest.class),
                    x -> (((ListClassResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), false);

            ResponseCode code = response.getCode();
            String message = Stringify.format(code);
            DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);

            if (response.getCode() != ResponseCode.OK) {
                return message;
            } else {
                DebugMessage.debug("Class state returned successfully.", null, DEBUG_FLAG);
                return Stringify.format(response.getClassState());
            }

        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught: " + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * "cancelEnrollments" client remote call facade
     * @param id
     * @return
     * @throws RuntimeException
     */
    public String cancelEnrollment(String id) throws RuntimeException {

        DebugMessage.debug("Calling remote call cancelEnrollment.", "cancelEnrollment", DEBUG_FLAG);
        CancelEnrollmentRequest request = CancelEnrollmentRequest.newBuilder().setStudentId(id).build();
        CancelEnrollmentResponse response;

        try {

            response = (CancelEnrollmentResponse) exchangeMessages(request,
                    ProfessorServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    ProfessorServiceGrpc.ProfessorServiceBlockingStub.class.getMethod("cancelEnrollment", CancelEnrollmentRequest.class),
                    x -> (((CancelEnrollmentResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);

        } catch (StatusRuntimeException e) {
            DebugMessage.debug("Runtime exception caught: " + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        super.shutdown();
    }

}
