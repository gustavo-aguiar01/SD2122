package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private Class studentClass;

    public StudentServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }
}
