package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
    private Class studentClass;

    public AdminServiceImpl(Class studentClass) {
        this.studentClass = studentClass;
    }

    @Override
    public void activate (ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        System.out.println("Executing activate");
        ActivateResponse response = ActivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate (DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {

        System.out.println("Executing deactivate");
        DeactivateResponse response = DeactivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void dump (DumpRequest request, StreamObserver<DumpResponse> responseObserver) {

        System.out.println("Executing dump");
        DumpResponse response = DumpResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
