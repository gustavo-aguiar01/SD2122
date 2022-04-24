package pt.ulisboa.tecnico.classes.admin;

import io.grpc.*;
import pt.ulisboa.tecnico.classes.ClientFrontend;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.concurrent.TimeUnit;

public class AdminFrontend extends ClientFrontend {

    // Set flag to true to print debug messages
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public AdminFrontend (String hostname, int port, String serviceName) {
        super(hostname, port, serviceName);
    }

    // Admin GRPC remote methods

    /**
     * "activate" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String activate(String primary) throws RuntimeException {

        DebugMessage.debug("Calling remote call activate.", "activate", DEBUG_FLAG);

        if (!(primary.equals("P") || primary.equals("S"))) {
            DebugMessage.debug("Invalid argument passed: " + primary + ".",
                    null, DEBUG_FLAG);
            throw new RuntimeException("Invalid argument passed: " + primary + ".");
        }

        // Refresh servers list
        try {
            super.refreshServers();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        StringBuilder builder = new StringBuilder();

        // Command usage: activate P/S
        for (ServerAddress sa : primary.equals("P") ? super.primaryServers : super.secondaryServers) {

            ManagedChannel channel = null;
            ActivateRequest request = ActivateRequest.getDefaultInstance();
            ActivateResponse response;
            String message;

            try {

                // Create communication channel with server address @ sa
                DebugMessage.debug("Creating communication channel with " + sa.getHost() + ":" + sa.getPort() + "...",
                        null, DEBUG_FLAG);
                channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
                AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
                response = stub.withDeadlineAfter(super.deadlineSecs, TimeUnit.SECONDS).activate(request);
                channel.shutdown();

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                DebugMessage.debug("Got the following response: " + message,
                        null, DEBUG_FLAG);
                builder.append(message).append("\n");

            } catch (StatusRuntimeException e){

                if (channel != null) { channel.shutdown(); }

                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available.", null, DEBUG_FLAG);
                    builder.append(Stringify.format(ResponseCode.INACTIVE_SERVER)); // Edge case where backup server closed after primary checked if servers size != 0
                }
                else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                    throw new RuntimeException(e.getStatus().getDescription());
                } else {
                    // Other than that it should throw exception
                    throw new RuntimeException(e.getStatus().getDescription());
                }
            }
        }
        return builder.toString();
    }

    /**
     * "deactivate" client remote call facade
     * @return String
     * @throws RuntimeException
     */
    public String deactivate(String primary) throws RuntimeException {

        DebugMessage.debug("Calling remote call deactivate.", "deactivate", DEBUG_FLAG);

        if (!(primary.equals("P") || primary.equals("S"))) {
            DebugMessage.debug("Invalid argument passed: " + primary + ".",
                    null, DEBUG_FLAG);
            throw new RuntimeException("Invalid argument passed: " + primary + ".");
        }

        // Refresh server list
        try {
            super.refreshServers();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        StringBuilder builder = new StringBuilder();

        for (ServerAddress sa : primary.equals("P") ? super.primaryServers : super.secondaryServers) {

            ManagedChannel channel = null;
            DeactivateRequest request = DeactivateRequest.getDefaultInstance();
            DeactivateResponse response;
            String message;

            try {

                // Create communication channel with server address @ sa
                DebugMessage.debug("Creating communication channel with " + sa.getHost() + ":" + sa.getPort() + "...",
                        null, DEBUG_FLAG);
                channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
                AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
                response = stub.withDeadlineAfter(super.deadlineSecs, TimeUnit.SECONDS).deactivate(request);
                channel.shutdown();

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                DebugMessage.debug("Got the following response: " + message,
                        null, DEBUG_FLAG);
                builder.append(message).append("\n");

            } catch (StatusRuntimeException e){

                if (channel != null) { channel.shutdown(); }

                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available.", null, DEBUG_FLAG);
                    builder.append(Stringify.format(ResponseCode.INACTIVE_SERVER)); // Edge case where backup server closed after primary checked if servers size != 0
                } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                    throw new RuntimeException(e.getStatus().getDescription());
                } else {
                    // Other than that it should throw exception
                    throw new RuntimeException(e.getStatus().getDescription());
                }
            }
        }
        return builder.toString();
    }

    /**
     * "dump" client remote call facade
     * @param primary
     * @return String
     * @throws RuntimeException
     */
    public String dump(String primary) throws RuntimeException {

        DebugMessage.debug("Calling remote call dump.", "dump", DEBUG_FLAG);

        if (!(primary.equals("P") || primary.equals("S"))) {
            DebugMessage.debug("Invalid argument passed: " + primary + ".",
                null, DEBUG_FLAG);
            throw new RuntimeException("Invalid argument passed: " + primary + ".");
        }

        // Refresh server list
        try {
            super.refreshServers();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        StringBuilder builder = new StringBuilder();

        for (ServerAddress sa : primary.equals("P") ? super.primaryServers : super.secondaryServers) {

            DebugMessage.debug("Dumping from " + (primary.equals("P") ? "primary" : "secondary") + " server @ " + sa.getHost() + ":" + sa.getPort() + ".",
                    null, DEBUG_FLAG);
            DumpRequest request = DumpRequest.getDefaultInstance();
            DumpResponse response;
            ManagedChannel channel = null;
            String message;

            try {

                // Create communication channel with server address @ sa
                DebugMessage.debug("Creating communication channel with " + sa.getHost() + ":" + sa.getPort() + "...",
                        null, DEBUG_FLAG);
                channel = ManagedChannelBuilder.forAddress(sa.getHost(), sa.getPort()).usePlaintext().build();
                AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
                response = stub.withDeadlineAfter(super.deadlineSecs, TimeUnit.SECONDS).dump(request);
                channel.shutdown();

                ResponseCode code = response.getCode();
                message = Stringify.format(code);
                DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);

                if (response.getCode() != ResponseCode.OK) {
                    builder.append(message).append("\n");
                } else {
                    DebugMessage.debug("Class state returned successfully.", null, DEBUG_FLAG);
                    builder.append(Stringify.format(response.getClassState()));
                }
            } catch (StatusRuntimeException e) {

                if (channel != null) { channel.shutdown(); }

                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No secondary servers available.", null, DEBUG_FLAG);
                    builder.append(Stringify.format(ResponseCode.INACTIVE_SERVER)); // Edge case where backup server closed after primary checked if servers size != 0
                } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                    throw new RuntimeException(e.getStatus().getDescription());
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
