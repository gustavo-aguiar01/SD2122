package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
    private Class studentClass;

    public AdminServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }
}
