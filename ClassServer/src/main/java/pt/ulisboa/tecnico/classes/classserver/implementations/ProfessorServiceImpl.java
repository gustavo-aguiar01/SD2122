package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.ClassStateReport;
import pt.ulisboa.tecnico.classes.classserver.ReplicaManager;
import pt.ulisboa.tecnico.classes.classserver.StateUpdate;
import pt.ulisboa.tecnico.classes.classserver.domain.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc.ProfessorServiceImplBase;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.List;

public class ProfessorServiceImpl extends ProfessorServiceImplBase {

    ReplicaManager replicaManager;

    public ProfessorServiceImpl(ReplicaManager serverState) {
        this.replicaManager = serverState;
    }

    /**
     * "openEnrollments" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver) {


        OpenEnrollmentsResponse response;
        ResponseCode code = ResponseCode.OK;

        try {

            if (request.getCapacity() < 0) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity input has to be a positive integer.").asRuntimeException());
                return;
            }

            replicaManager.issueUpdate(
                    new StateUpdate("openEnrollments",
                            List.of(Integer.toString(request.getCapacity())),
                            new Timestamp(request.getTimestampMap())), false);

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (EnrollmentsAlreadyOpenException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_OPENED;
        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } catch (UpdateIssuedException e) {
            code = ResponseCode.UPDATE_ISSUED;
        } catch (ClassDomainException e) {
            ; /* this is never reached since all relevant exceptions which inherit this are already handled */
        }

        response = OpenEnrollmentsResponse.newBuilder().setCode(code).build();
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
            replicaManager.issueUpdate(
                    new StateUpdate("closeEnrollments", List.of(),
                            new Timestamp(request.getTimestampMap())), false);

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } catch (UpdateIssuedException e) {
            code = ResponseCode.UPDATE_ISSUED;
        } catch (ClassDomainException e) {
            ; /* this is never reached since all relevant exceptions which inherit this are already handled */
        }

        response = CloseEnrollmentsResponse.newBuilder().setCode(code).build();
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

            ClassStateReport studentClass = replicaManager.reportClassState(false);

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudents()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudents()))
                    .build();

            response = ListClassResponse.newBuilder().setCode(code)
                    .setClassState(state).putAllTimestamp(studentClass.getTimestamp().getMap()).build();

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
            replicaManager.issueUpdate(
                    new StateUpdate("cancelEnrollment", List.of(request.getStudentId()),
                            new Timestamp(request.getTimestampMap())), false);

        } catch (InactiveServerException e)  {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (NonExistingStudentException e) {
            code = ResponseCode.NON_EXISTING_STUDENT;
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } catch (UpdateIssuedException e) {
            code = ResponseCode.UPDATE_ISSUED;
        } catch (ClassDomainException e) {
            ; /* this is never reached since all relevant exceptions which inherit this are already handled */
        }

        response = CancelEnrollmentResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
