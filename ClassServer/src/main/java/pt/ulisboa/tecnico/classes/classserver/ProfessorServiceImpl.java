package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {
    ClassServer.ClassServerState serverState;

    public ProfessorServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }
}
