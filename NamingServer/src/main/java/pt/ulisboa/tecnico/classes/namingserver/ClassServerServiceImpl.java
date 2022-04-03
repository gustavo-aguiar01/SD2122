package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerServiceGrpc.ClassServerServiceImplBase;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.namingserver.domain.NamingServices;

public class ClassServerServiceImpl extends ClassServerServiceImplBase {

    NamingServices services = new NamingServices();

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        services.addService(request.getServiceName(), request.getHost(), request.getPort(), request.getQualifiersList());

        responseObserver.onNext(RegisterResponse.getDefaultInstance());
        responseObserver.onCompleted();

    }
}
