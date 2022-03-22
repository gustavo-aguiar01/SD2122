package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private ClassServer.ClassServerState serverState;

    public StudentServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }
}
