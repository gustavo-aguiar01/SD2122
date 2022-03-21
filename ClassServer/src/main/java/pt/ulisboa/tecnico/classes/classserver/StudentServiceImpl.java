package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private Class studentClass;

    public StudentServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }

}
