package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

import java.util.List;
import java.util.stream.Collectors;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {

    ClassServer.ClassServerState serverState;

    public ProfessorServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }
 
    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver) {
        if (request.getCapacity() < 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity input has to be a positive integer!").asRuntimeException());
            return;
        }
        OpenEnrollmentsResponse response;
        if (!serverState.isActive()) {
            response = OpenEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.INACTIVE_SERVER).build();
        } else if (serverState.getStudentClass().areRegistrationsOpen()) {
            response = OpenEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.ENROLLMENTS_ALREADY_OPENED).build();
        } else {
            serverState.getStudentClass().openEnrollments(request.getCapacity());
            response = OpenEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request, StreamObserver<CloseEnrollmentsResponse> responseObserver) {
        CloseEnrollmentsResponse response;
        if (!serverState.isActive()) {
            response = CloseEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.INACTIVE_SERVER).build();
        } else if (!serverState.getStudentClass().areRegistrationsOpen()) {
            response = CloseEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.ENROLLMENTS_ALREADY_CLOSED).build();
        } else {
            serverState.getStudentClass().closeEnrollments();
            response = CloseEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        ListClassResponse response;
        if (!serverState.isActive()) {
            response = ListClassResponse.newBuilder()
                    .setCode(ResponseCode.INACTIVE_SERVER).build();
        } else {
            List<Student> enrolledStudents = serverState.getStudentClass().getEnrolledStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());

            List<Student> discardedStudents = serverState.getStudentClass().getRevokedStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());

            ClassState state = ClassState.newBuilder().setCapacity(serverState.getStudentClass().getCapacity())
                    .setOpenEnrollments(serverState.getStudentClass().areRegistrationsOpen())
                    .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();
            response = ListClassResponse.newBuilder().setCode(ResponseCode.OK)
                    .setClassState(state).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void cancelEnrollment(CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver) {
        CancelEnrollmentResponse response;
        if (!ClassStudent.isValidStudentId(request.getStudentId())) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer).").asRuntimeException());
            return;
        } else if (!serverState.isActive()) {
            response = CancelEnrollmentResponse.newBuilder()
                    .setCode(ResponseCode.INACTIVE_SERVER).build();
        } else if (!serverState.getStudentClass().isStudentEnrolled(request.getStudentId())) {
            response = CancelEnrollmentResponse.newBuilder()
                    .setCode(ResponseCode.NON_EXISTING_STUDENT).build();
        } else {
            serverState.getStudentClass().revokeEnrollment(request.getStudentId());
            response = CancelEnrollmentResponse.newBuilder()
                    .setCode(ResponseCode.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
