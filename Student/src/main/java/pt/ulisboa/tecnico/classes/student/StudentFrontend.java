package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentFrontend {

    StudentServiceGrpc.StudentServiceBlockingStub stub;

    public StudentFrontend(ManagedChannel channel) {
        this.stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    public String enroll(String id, String name) throws StatusRuntimeException {
        Student newStudent = Student.newBuilder().setStudentId(id).setStudentName(name).build();
        return Stringify.format(stub.enroll(EnrollRequest.newBuilder().setStudent(newStudent).build()).getCode());
    }

    public String listClass() {
        ListClassResponse response = stub.listClass(ListClassRequest.getDefaultInstance());
        if (response.getCode() != ClassesDefinitions.ResponseCode.OK) {
            return Stringify.format(response.getCode());
        } else {
            return Stringify.format(response.getClassState());
        }
    }

}
