package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.ReplicaManager;
import pt.ulisboa.tecnico.classes.classserver.StateUpdate;
import pt.ulisboa.tecnico.classes.classserver.domain.*;
import pt.ulisboa.tecnico.classes.classserver.ClassStateReport;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc.StudentServiceImplBase;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;


import java.util.Arrays;

import static io.grpc.Status.INVALID_ARGUMENT;

public class StudentServiceImpl extends StudentServiceImplBase {

    private ReplicaManager replicaManager;

    public StudentServiceImpl(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    /**
     * "enroll" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {

        if (!ClassStudent.isValidStudentId(request.getStudent().getStudentId())) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer).")
                    .asRuntimeException());
            return;
        } else if (!ClassStudent.isValidStudentName((request.getStudent().getStudentName()))){
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid student name input! Student name should have from 3 to 30 characters including spaces.")
                    .asRuntimeException());
            return;
        }

        EnrollResponse response;
        ResponseCode code = ResponseCode.OK;
        Timestamp timestamp = new Timestamp();

        try {
            timestamp = replicaManager.issueUpdate(new StateUpdate("enroll",
                    Arrays.asList(request.getStudent().getStudentId(), request.getStudent().getStudentName()),
                            new Timestamp(request.getWriteTimestampMap())),
                    new Timestamp(request.getReadTimestampMap()), false);
        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
            timestamp = e.getTimestamp();
        } catch (StudentAlreadyEnrolledException e) {
            code = ResponseCode.STUDENT_ALREADY_ENROLLED;
            timestamp = e.getTimestamp();
        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;
            timestamp = e.getTimestamp();
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } catch (UpdateIssuedException e) {
            code = ResponseCode.UPDATE_ISSUED;
            timestamp = e.getTimestamp();
        } catch (ClassDomainException e) {
            ; /* this is never reached since all relevant exceptions which inherit this are already handled */
        }

        response = EnrollResponse.newBuilder().setCode(code).putAllTimestamp(timestamp.getMap()).build();
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

            response = StudentClassServer.ListClassResponse.newBuilder().setCode(code)
                    .setClassState(state).putAllTimestamp(studentClass.getTimestamp().getMap()).build();

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
            response = ListClassResponse.newBuilder()
                    .setCode(code).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
