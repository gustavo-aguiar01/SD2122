package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;

import java.util.stream.Collectors;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private Class studentClass;

    public StudentServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {

        if (ClassStudent.isValidStudentId(request.getStudent().getStudentId()) == false) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid student id input! Format: alunoXXXX (each X is a positive integer)")
                    .asRuntimeException());
        } else {
            EnrollResponse response;
            if (studentClass.contains(request.getStudent().getStudentId()) == true) {
                response = EnrollResponse.newBuilder().setCode(ResponseCode.STUDENT_ALREADY_ENROLLED).build();
            } else {
                ClassStudent newStudent = new ClassStudent(request.getStudent().getStudentId(),
                        request.getStudent().getStudentName());
                studentClass.enroll(newStudent);
                response = EnrollResponse.newBuilder().setCode(ResponseCode.OK).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        List<Student> enrolledStudents = studentClass.getEnrolledStudentsCollection().stream()
                .map(s -> Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        List<Student> discardedStudents = studentClass.getRevokedStudentsCollection().stream()
                                .map(s -> Student.newBuilder().setStudentId(s.getId())
                                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                        .setOpenEnrollments(studentClass.isOpenRegistrations())
                                .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();

        responseObserver.onNext(ListClassResponse.newBuilder().setCode(ResponseCode.OK)
                .setClassState(state).build());
        responseObserver.onCompleted();
    }

}
