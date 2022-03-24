package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;


import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;

import java.util.stream.Collectors;
import java.util.List;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
    ClassServer.ClassServerState serverState;

    public AdminServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void activate (ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        serverState.setActive(true);
        ActivateResponse response = ActivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate (DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {

        serverState.setActive(false);
        DeactivateResponse response = DeactivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void dump (DumpRequest request, StreamObserver<DumpResponse> responseObserver) {

        DumpResponse response;
        ResponseCode code = ResponseCode.OK;

        try {
            Class studentClass = serverState.getStudentClass();
            ClassState state = studentClass.getClassState();

            response = DumpResponse.newBuilder().setCode(code)
                    .setClassState(state).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;

        }

        response = DumpResponse.newBuilder()
                .setCode(code).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
