package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;

import java.util.stream.Collectors;
import java.util.List;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private Class studentClass;

    public StudentServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        studentClass.enroll(new ClassStudent(request.getStudent().getStudentId(),
                request.getStudent().getStudentName()));
        responseObserver.onNext(EnrollResponse.getDefaultInstance());
        responseObserver.onCompleted();
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
