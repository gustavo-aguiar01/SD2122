package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {

    ClassServer.ClassServerState serverState;
    private Class professorClass;

    public ProfessorServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }
 
    @Override
    public void openEnrollments(ProfessorClassServer.OpenEnrollmentsRequest request, StreamObserver<ProfessorClassServer.OpenEnrollmentsResponse> responseObserver) {
        if (request.getCapacity() < 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity input has to be a positive integer!").asRuntimeException());
        } else {
            ProfessorClassServer.OpenEnrollmentsResponse response = ProfessorClassServer.OpenEnrollmentsResponse.newBuilder()
                    .setCode(professorClass.openEnrollments(request.getCapacity())).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
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
        if (ClassStudent.isValidStudentId(request.getStudentId()) == false) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer).").asRuntimeException());
        } else {
            ProfessorClassServer.CancelEnrollmentResponse response = ProfessorClassServer.CancelEnrollmentResponse.newBuilder()
                    .setCode(professorClass.cancelEnrollment(request.getStudentId())).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}
