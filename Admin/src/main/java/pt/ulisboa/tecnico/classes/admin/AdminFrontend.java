package pt.ulisboa.tecnico.classes.admin;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.security.MessageDigest;

public class AdminFrontend {

    private AdminServiceGrpc.AdminServiceBlockingStub stub;
    private final ManagedChannel channel;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public AdminFrontend (String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(this.channel);
    }

    // admin remote methods
    public String activate() throws RuntimeException {
        try {
            DebugMessage.debug("Calling remote call ativate", "activate", DEBUG_FLAG);
            ResponseCode responseCode = stub.activate(ActivateRequest.getDefaultInstance()).getCode();

            String message = Stringify.format(responseCode);
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);

            return message;
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public String deactivate() throws RuntimeException {
        try {
            DebugMessage.debug("Calling remote call deactivate", "deactivate", DEBUG_FLAG);
            ResponseCode responseCode = stub.deactivate(DeactivateRequest.getDefaultInstance()).getCode();

            String message = Stringify.format(responseCode);
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);
            return message;
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public String dump() throws RuntimeException {

        try {
            DebugMessage.debug("Calling remote call dump", "dump", DEBUG_FLAG);
            DumpResponse response = stub.dump(DumpRequest.getDefaultInstance());
            ResponseCode code = response.getCode();
            DebugMessage.debug("Got the following response code : " + Stringify.format(code), null, DEBUG_FLAG);

            String message;
            if (code == ResponseCode.OK) {
                message = Stringify.format(response.getClassState());
                DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);

            } else {
                message = Stringify.format(code);
            }

            return message;
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public void shutdown() {
        channel.shutdown();
    }
}
