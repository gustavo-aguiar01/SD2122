package pt.ulisboa.tecnico.classes.admin;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AdminFrontend {

    private AdminServiceGrpc.AdminServiceBlockingStub stub;
    private final ManagedChannel channel;

    public AdminFrontend (String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(this.channel);
    }

    // admin remote methods
    public String activate () {

        ResponseCode responseCode = stub.activate(ActivateRequest.getDefaultInstance()).getCode();
        String message = Stringify.format(responseCode);

        return message;
    }

    public String deactivate () {

        ResponseCode responseCode = stub.deactivate(DeactivateRequest.getDefaultInstance()).getCode();
        String message = Stringify.format(responseCode);

        return message;
    }

    public String dump () {

        DumpResponse response = stub.dump(DumpRequest.getDefaultInstance());
        String message = Stringify.format(response.getCode());
        String classState = Stringify.format(response.getClassState());

        return message + " " + classState;
    }
}
