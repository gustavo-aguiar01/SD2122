package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorFrontend {
    private final ProfessorServiceGrpc.ProfessorServiceBlockingStub stub;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ProfessorFrontend(ManagedChannel channel) {
        stub = ProfessorServiceGrpc.newBlockingStub(channel);
    }

    public String openEnrollments(int capacity) {

        DebugMessage.debug("Calling remote call openEnrollments", "openEnrollments", DEBUG_FLAG);
        ProfessorClassServer.OpenEnrollmentsResponse responseOpenEnrollments = stub.openEnrollments(
                ProfessorClassServer.OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build());

        String message = Stringify.format(responseOpenEnrollments.getCode());
        DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
        return message;
    }

    public String closeEnrollments() {

        DebugMessage.debug("Calling remote call closeEnrollments", "closeEnrollments", DEBUG_FLAG);
        ProfessorClassServer.CloseEnrollmentsResponse responseCloseEnrollments = stub.closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest.getDefaultInstance());

        String message = Stringify.format(responseCloseEnrollments.getCode());
        DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
        return message;
    }

    public String listClass() {

        DebugMessage.debug("Calling remote call listClass", "listClass", DEBUG_FLAG);
        ProfessorClassServer.ListClassResponse response = stub.listClass(ProfessorClassServer.ListClassRequest.getDefaultInstance());

        String message = Stringify.format(response.getCode());
        DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
        if (response.getCode() != ClassesDefinitions.ResponseCode.OK) {
            return Stringify.format(response.getCode());
        }

        DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);
        return Stringify.format(response.getClassState());
    }

    public String cancelEnrollment(String id) {

        DebugMessage.debug("Calling remote call cancelEnrollment", "cancelEnrollment", DEBUG_FLAG);
        ProfessorClassServer.CancelEnrollmentResponse responseCancelEnrollments = stub.cancelEnrollment(
                ProfessorClassServer.CancelEnrollmentRequest.newBuilder().setStudentId(id).build());

        String message = Stringify.format(responseCancelEnrollments.getCode());
        DebugMessage.debug("Got the following response code : " + message, null, DEBUG_FLAG);
        return message;
    }

}
