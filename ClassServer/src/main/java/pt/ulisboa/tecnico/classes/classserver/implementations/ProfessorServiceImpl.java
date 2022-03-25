package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc.ProfessorServiceImplBase;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.List;
import java.util.stream.Collectors;

public class ProfessorServiceImpl extends ProfessorServiceImplBase {

    ClassServer.ClassServerState serverState;

    public ProfessorServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    /**
     * "openEnrollments" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver) {

        if (request.getCapacity() < 0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity input has to be a positive integer!").asRuntimeException());
            return;
        }

        OpenEnrollmentsResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass(false);
            studentClass.openEnrollments(request.getCapacity());

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        } catch (EnrollmentsAlreadyOpenException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_OPENED;

        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;

        }

        response = OpenEnrollmentsResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * "closeEnrollments" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request, StreamObserver<CloseEnrollmentsResponse> responseObserver) {

        CloseEnrollmentsResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass(false);
            studentClass.closeEnrollments();

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;

        }

        response = CloseEnrollmentsResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * "list" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {

        ListClassResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass(false);

            // Construct ClassState
            List<Student> enrolledStudents = studentClass.getEnrolledStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());

            List<Student> discardedStudents = studentClass.getRevokedStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();

            response = ListClassResponse.newBuilder().setCode(code)
                    .setClassState(state).build();


        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
            response = ListClassResponse.newBuilder()
                    .setCode(code).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * "cancelEnrollment" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void cancelEnrollment(CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver) {

        CancelEnrollmentResponse response;
        ResponseCode code = ResponseCode.OK;

        if (!ClassStudent.isValidStudentId(request.getStudentId())) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer).").asRuntimeException());
            return;
        }

        try {
            Class studentClass = serverState.getStudentClass(false);
            studentClass.revokeEnrollment(request.getStudentId());

        } catch (InactiveServerException e)  {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (NonExistingStudentException e) {
            code = ResponseCode.NON_EXISTING_STUDENT;
        }

        response = CancelEnrollmentResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
