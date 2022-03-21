package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {
    private Class studentClass;

    public ProfessorServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }
}
