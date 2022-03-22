package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;

import java.util.List;
import java.util.stream.Collectors;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {

    ClassServer.ClassServerState serverState;

    public ProfessorServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }
 
    @Override
    public void openEnrollments(ProfessorClassServer.OpenEnrollmentsRequest request, StreamObserver<ProfessorClassServer.OpenEnrollmentsResponse> responseObserver) {
        if (request.getCapacity() < 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity input has to be a positive integer!").asRuntimeException());
        } else {
            ProfessorClassServer.OpenEnrollmentsResponse response;
            if (professorClass.areRegistrationsOpen() == true) {
                response = ProfessorClassServer.OpenEnrollmentsResponse.newBuilder()
                        .setCode(ResponseCode.ENROLLMENTS_ALREADY_OPENED).build();
            } else {
                professorClass.openEnrollments(request.getCapacity());
                response = ProfessorClassServer.OpenEnrollmentsResponse.newBuilder()
                        .setCode(ResponseCode.OK).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest request, StreamObserver<ProfessorClassServer.CloseEnrollmentsResponse> responseObserver) {
        ProfessorClassServer.CloseEnrollmentsResponse response;
        if (professorClass.areRegistrationsOpen() == false) {
            response = ProfessorClassServer.CloseEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.ENROLLMENTS_ALREADY_CLOSED).build();
        } else {
            professorClass.closeEnrollments();
            response = ProfessorClassServer.CloseEnrollmentsResponse.newBuilder()
                    .setCode(ResponseCode.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listClass(ProfessorClassServer.ListClassRequest request, StreamObserver<ProfessorClassServer.ListClassResponse> responseObserver) {
        List<Student> enrolledStudents = professorClass.getEnrolledStudentsCollection().stream()
                .map(s -> Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        List<Student> discardedStudents = professorClass.getRevokedStudentsCollection().stream()
                .map(s -> Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        ClassState state = ClassState.newBuilder().setCapacity(professorClass.getCapacity())
                .setOpenEnrollments(professorClass.areRegistrationsOpen())
                .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();
        responseObserver.onNext(ProfessorClassServer.ListClassResponse.newBuilder().setCode(ResponseCode.OK)
                .setClassState(state).build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelEnrollment(ProfessorClassServer.CancelEnrollmentRequest request, StreamObserver<ProfessorClassServer.CancelEnrollmentResponse> responseObserver) {
        if (ClassStudent.isValidStudentId(request.getStudentId()) == false) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer).").asRuntimeException());
        } else {
            ProfessorClassServer.CancelEnrollmentResponse response;
            if (professorClass.isStudentEnrolled(request.getStudentId()) == false) {
                response = ProfessorClassServer.CancelEnrollmentResponse.newBuilder()
                        .setCode(ResponseCode.NON_EXISTING_STUDENT).build();
            } else {
                professorClass.revokeEnrollment(request.getStudentId());
                response = ProfessorClassServer.CancelEnrollmentResponse.newBuilder()
                        .setCode(ResponseCode.OK).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
