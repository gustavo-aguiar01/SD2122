package pt.ulisboa.tecnico.classes.admin;

import io.grpc.*;
import pt.ulisboa.tecnico.classes.ClientFrontend;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.ArrayList;
import java.util.List;


public class AdminFrontend extends ClientFrontend {

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public AdminFrontend (String hostname, int port, String serviceName) {
        super(hostname, port, serviceName);
    }

    // admin remote methods

    /**
     * "activate" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String activate() throws RuntimeException {

        DebugMessage.debug("Calling remote call ativate", "activate", DEBUG_FLAG);
        ActivateRequest request = ActivateRequest.getDefaultInstance();
        ActivateResponse response;

        try {
            response = (ActivateResponse) exchangeMessages(request,
                    AdminServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    AdminServiceGrpc.AdminServiceBlockingStub.class.getMethod("activate", ActivateRequest.class),
                    x -> (((ActivateResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * "deactivate" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String deactivate() throws RuntimeException {

        DebugMessage.debug("Calling remote call deactivate", "deactivate", DEBUG_FLAG);
        DeactivateRequest request = DeactivateRequest.getDefaultInstance();
        DeactivateResponse response;

        try {
            response = (DeactivateResponse) exchangeMessages(request,
                    AdminServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    AdminServiceGrpc.AdminServiceBlockingStub.class.getMethod("deactivate", DeactivateRequest.class),
                    x -> (((DeactivateResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), true);
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }

        return Stringify.format(response.getCode());

    }

    /**
     * "dump" client remote call facade
     * @param primary
     * @return String
     * @throws RuntimeException
     */
    public String dump(String primary) throws RuntimeException {

        DebugMessage.debug("Calling remote call dump", "dump", DEBUG_FLAG);

        try {
            super.refreshServers();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        StringBuilder builder = new StringBuilder();

        for (ServerAddress sa : primary.equals("P") ? super.writeServers : super.readServers) {

            DebugMessage.debug("Dumping from " + (primary.equals("P") ? "primary" : "secondary") + " server @ " + sa.getHost() + ":" + sa.getPort(),
                    null, DEBUG_FLAG);
            DumpRequest request = DumpRequest.getDefaultInstance();
            DumpResponse response;
            String message;

            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
                AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
                response = stub.dump(request);

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
                if (response.getCode() != ResponseCode.OK) {
                    builder.append(message + "\n");
                } else {
                    DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);
                    builder.append(Stringify.format(response.getClassState()));
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available!", null, DEBUG_FLAG);
                    builder.append(Stringify.format(ResponseCode.INACTIVE_SERVER)); // Edge case where backup server closed after primary checked if servers size != 0
                } else {
                    // Other than that it should throw exception
                    throw new RuntimeException(e.getStatus().getDescription());
                }
            }
        }
    return builder.toString();
    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        super.shutdown();
    }
}
