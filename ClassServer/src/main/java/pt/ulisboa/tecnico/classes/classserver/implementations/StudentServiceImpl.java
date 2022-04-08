package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.classserver.domain.*;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.ClassServer;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassStateReport;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc.StudentServiceImplBase;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import static io.grpc.Status.INVALID_ARGUMENT;

public class StudentServiceImpl extends StudentServiceImplBase {

    private ClassServer.ClassServerState serverState;

    public StudentServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
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
        int versionNumber = -1;

        try {
            Class studentClass = serverState.getStudentClassToWrite(false);
            versionNumber = serverState.getStudentClass(false).getVersionNumber();

            String id = request.getStudent().getStudentId();
            String name = request.getStudent().getStudentName();
            ClassStudent student = new ClassStudent(id, name);

            studentClass.enroll(student);
            versionNumber = studentClass.getVersionNumber();

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
        } catch (StudentAlreadyEnrolledException e) {
            code = ResponseCode.STUDENT_ALREADY_ENROLLED;
        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;
        } catch (InvalidOperationException e) {
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        }

        response = EnrollResponse.newBuilder().setCode(code).setVersionNumber(versionNumber).build();
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

            if (request.getVersionNumber() > serverState.getStudentClass(false).getVersionNumber()) {
                responseObserver.onNext(ListClassResponse.newBuilder().setCode(ResponseCode.UNDER_MAINTENANCE).build());
                responseObserver.onCompleted();
                return ;
            }

            ClassStateReport studentClass = serverState.getStudentClass(false).reportClassState();

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudents()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudents()))
                    .build();

            response = StudentClassServer.ListClassResponse.newBuilder().setCode(code)
                    .setClassState(state).setVersionNumber(studentClass.getVersionNumber()).build();

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
            response = ListClassResponse.newBuilder()
                    .setCode(code).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
