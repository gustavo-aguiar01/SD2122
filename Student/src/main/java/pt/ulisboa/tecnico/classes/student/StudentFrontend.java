package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentFrontend {

    StudentServiceGrpc.StudentServiceBlockingStub stub;

    public StudentFrontend(ManagedChannel channel) {
        this.stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    public void enroll(String id, String name) {
        Student newStudent = Student.newBuilder().setStudentId(id).setStudentName(name).build();
        stub.enroll(EnrollRequest.newBuilder().setStudent(newStudent).build());
    }

    public String listClass() {
        ListClassResponse response = stub.listClass(ListClassRequest.getDefaultInstance());
        String res = "";
        res += response.getCodeValue();
        res += '\n';
        res += "Inscritos:\n";
        if (response.getClassState().getEnrolledList().size() == 0) {
            res += "VAZIO\n";
        } else {
            for (Student s : response.getClassState().getEnrolledList()) {
                res += "- ";
                res += s.getStudentId();
                res += " ";
                res += s.getStudentName();
                res += '\n';
            }
        }
        res += "Cancelados:\n";
        if (response.getClassState().getDiscardedList().size() == 0) {
            res += "VAZIO\n";
        } else {
            for (Student s : response.getClassState().getDiscardedList()) {
                res += "- ";
                res += s.getStudentId();
                res += " ";
                res += s.getStudentName();
                res += '\n';
            }
        }
        return res;
    }
}
