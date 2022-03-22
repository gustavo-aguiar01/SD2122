package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;

public class ProfessorFrontend {
    private final ProfessorServiceGrpc.ProfessorServiceBlockingStub stub;

    public ProfessorFrontend() {
        final String host = "127.0.0.1";
        final int port = 8080;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ProfessorServiceGrpc.newBlockingStub(channel);
    }

    public String openEnrollments(int capacity) {
        ProfessorClassServer.OpenEnrollmentsResponse responseOpenEnrollments = stub.openEnrollments(
                ProfessorClassServer.OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build());
        return Stringify.format(responseOpenEnrollments.getCode());
    }

    public String closeEnrollments() {
        ProfessorClassServer.CloseEnrollmentsResponse responseCloseEnrollments = stub.closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest.getDefaultInstance());
        return Stringify.format(responseCloseEnrollments.getCode());
    }

    public String listClass() {
        ProfessorClassServer.ListClassResponse response = stub.listClass(ProfessorClassServer.ListClassRequest.getDefaultInstance());
        if (response.getCode() != ClassesDefinitions.ResponseCode.OK) {
            return Stringify.format(response.getCode());
        }
        return Stringify.format(response.getClassState());
    }

    public String cancelEnrollment(String id) {
        ProfessorClassServer.CancelEnrollmentResponse responseCancelEnrollments = stub.cancelEnrollment(
                ProfessorClassServer.CancelEnrollmentRequest.newBuilder().setStudentId(id).build());
        return Stringify.format(responseCancelEnrollments.getCode());
    }

}
