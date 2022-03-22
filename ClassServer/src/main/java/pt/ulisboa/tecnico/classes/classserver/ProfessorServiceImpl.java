package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {
    private Class professorClass;

    public ProfessorServiceImpl(Class professorClass) {
        this.professorClass = professorClass;
    }

    @Override
    public void openEnrollments(ProfessorClassServer.OpenEnrollmentsRequest request, StreamObserver<ProfessorClassServer.OpenEnrollmentsResponse> responseObserver) {
        ProfessorClassServer.OpenEnrollmentsResponse response = ProfessorClassServer.OpenEnrollmentsResponse.newBuilder()
                .setCode(professorClass.openEnrollments(request.getCapacity())).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest request, StreamObserver<ProfessorClassServer.CloseEnrollmentsResponse> responseObserver) {
        ProfessorClassServer.CloseEnrollmentsResponse response = ProfessorClassServer.CloseEnrollmentsResponse.newBuilder()
                .setCode(professorClass.closeEnrollments()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void cancelEnrollment(ProfessorClassServer.CancelEnrollmentRequest request, StreamObserver<ProfessorClassServer.CancelEnrollmentResponse> responseObserver) {
        ProfessorClassServer.CancelEnrollmentResponse response = ProfessorClassServer.CancelEnrollmentResponse.newBuilder()
                .setCode(professorClass.cancelEnrollment(request.getStudentId())).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
