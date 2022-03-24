package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;

import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;

import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

import java.util.stream.Collectors;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private ClassServer.ClassServerState serverState;

    public StudentServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {

        if (ClassStudent.isValidStudentId(request.getStudent().getStudentId()) == false) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer)")
                    .asRuntimeException());
        } else if (ClassStudent.isValidStudentName((request.getStudent().getStudentName())) == false){
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid student name input! Student name should have from 3 to 30 characters " +
                            "including spaces")
                    .asRuntimeException());
        }

        EnrollResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass();

            String id = request.getStudent().getStudentId();
            String name = request.getStudent().getStudentName();
            ClassStudent student = new ClassStudent(id, name);

            studentClass.enroll(student);

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        } catch (EnrollmentsAlreadyClosedException e) {
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;

        } catch (StudentAlreadyEnrolledException e) {
            code = ResponseCode.STUDENT_ALREADY_ENROLLED;

        } catch (FullClassException e) {
            code = ResponseCode.FULL_CLASS;

        }

        response = EnrollResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {

        ListClassResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass();
            ClassState state = studentClass.getClassState();

            response = ListClassResponse.newBuilder().setCode(code)
                    .setClassState(state).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        }

        response = ListClassResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
