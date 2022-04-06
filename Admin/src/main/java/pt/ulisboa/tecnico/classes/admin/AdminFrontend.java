package pt.ulisboa.tecnico.classes.admin;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.ClientFrontend;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;


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
     * @return String
     * @throws RuntimeException
     */
    public String dump() throws RuntimeException {

        DebugMessage.debug("Calling remote call dump", "dump", DEBUG_FLAG);
        DumpRequest request = DumpRequest.getDefaultInstance();
        DumpResponse response;

        try {
            response = (DumpResponse) exchangeMessages(request,
                    AdminServiceGrpc.class.getMethod("newBlockingStub", Channel.class),
                    AdminServiceGrpc.AdminServiceBlockingStub.class.getMethod("dump", DumpRequest.class),
                    x -> (((DumpResponse)x).getCode().equals(ResponseCode.INACTIVE_SERVER)), false);

            ResponseCode code = response.getCode();
            String message = Stringify.format(code);
            DebugMessage.debug("Got the following response : " + message, null, DEBUG_FLAG);

            if (response.getCode() != ResponseCode.OK) {
                return message;
            } else {
                DebugMessage.debug("Class state returned successfully", null, DEBUG_FLAG);
                return Stringify.format(response.getClassState());
            }
        } catch (StatusRuntimeException e){
            DebugMessage.debug("Runtime exception caught :" + e.getStatus().getDescription(), null, DEBUG_FLAG);
            throw new RuntimeException(e.getStatus().getDescription());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Communication channel shutdown function
     */
    public void shutdown() {
        super.shutdown();
    }
}
