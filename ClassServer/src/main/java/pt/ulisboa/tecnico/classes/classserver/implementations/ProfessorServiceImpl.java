package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.classserver.ClassServer;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassStudent;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassUtilities;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc.ProfessorServiceImplBase;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;


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
            Class studentClass = serverState.getStudentClassToWrite(false);
            studentClass.openEnrollments(request.getCapacity());

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        } catch (EnrollmentsAlreadyOpenException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_OPENED;

        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;

        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
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
            Class studentClass = serverState.getStudentClassToWrite(false);
            studentClass.closeEnrollments();

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;

        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
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

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudentsCollection()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudentsCollection()))
                    .build();

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
            Class studentClass = serverState.getStudentClassToWrite(false);
            studentClass.revokeEnrollment(request.getStudentId());

        } catch (InactiveServerException e)  {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (NonExistingStudentException e) {
            code = ResponseCode.NON_EXISTING_STUDENT;
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        }

        response = CancelEnrollmentResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
