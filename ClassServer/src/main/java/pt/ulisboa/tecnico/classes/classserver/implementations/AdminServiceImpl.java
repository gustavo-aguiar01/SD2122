package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.classserver.*;
import pt.ulisboa.tecnico.classes.classserver.ClassStateReport;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassUtilities;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;

import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc.AdminServiceImplBase;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.grpc.Status.INVALID_ARGUMENT;


public class AdminServiceImpl extends AdminServiceImplBase {

    ReplicaManager replicaManager;
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public AdminServiceImpl(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    /**
     * "activate" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void activate (ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        replicaManager.setActive(true);
        ActivateResponse response = ActivateResponse.newBuilder().setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * "deactivate" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void deactivate (DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {

        replicaManager.setActive(false);
        DeactivateResponse response = DeactivateResponse.newBuilder().setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * "dump" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void dump(DumpRequest request, StreamObserver<DumpResponse> responseObserver) {

        DumpResponse response;
        ResponseCode code = ResponseCode.OK;
        try {
            ClassStateReport studentClass = replicaManager.getStudentClass(true).reportClassState();

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudents()))
                    .addAllDiscarded(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudents()))
                    .build();

            response = DumpResponse.newBuilder().setCode(code)
                    .setClassState(state).build();
        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
            response = DumpResponse.newBuilder()
                    .setCode(code).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void activateGossip (ActivateGossipRequest request, StreamObserver<ActivateGossipResponse> responseObserver) {

        replicaManager.setActiveGossip(true);
        ActivateGossipResponse response = ActivateGossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void deactivateGossip (DeactivateGossipRequest request, StreamObserver<DeactivateGossipResponse> responseObserver) {

        replicaManager.setActiveGossip(false);
        DeactivateGossipResponse response = DeactivateGossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void gossip (GossipRequest request, StreamObserver<GossipResponse> responseObserver) {

        DebugMessage.debug("Forcing gossip.", "gossip", DEBUG_FLAG);

        // Propagate state to secondary servers
        // TODO : dont hardcode serviceName
        String serviceName = "Turmas";

        ClassServerNamingServer.LookupRequest lookupRequest = ClassServerNamingServer.LookupRequest.newBuilder()
                .setServiceName(serviceName).addAllQualifiers(new ArrayList<>()).build();
        List<ClassServerNamingServer.ServerAddress> servers;

        // TOOO : dont hardcode hostname and port
        String nameServerHostname = "localhost";
        int nameServerPort = 5000;

        ManagedChannel channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext().build();
        ClassNamingServerServiceGrpc.ClassNamingServerServiceBlockingStub namingServerStub = ClassNamingServerServiceGrpc.newBlockingStub(channel);

        try {
            // Lookup secondary servers
            servers = namingServerStub.withDeadlineAfter(5, TimeUnit.SECONDS).lookup(lookupRequest).getServersList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }

        servers.forEach(sa -> { if (!replicaManager.getValueTimestamp()
                .contains(sa.getHost() + ":" + sa.getPort())) {
            replicaManager.addReplica(sa.getHost(), sa.getPort()); }});

        //ClassStateReport studentClass = replicaManager.reportClassState(false);
        LogReport logReport = replicaManager.reportLogRecords();
        Collection<ClassesDefinitions.LogRecord> logRecords = logReport.getLogRecords().stream()
                .map(lr -> {
                    ClassesDefinitions.LogStatus status = ClassesDefinitions.LogStatus.SUCCESS;
                    switch (lr.getStatus()) {
                        case SUCCESS -> status = ClassesDefinitions.LogStatus.SUCCESS;
                        case FAIL -> status = ClassesDefinitions.LogStatus.FAIL;
                        case NONE -> status = ClassesDefinitions.LogStatus.NONE;
                    }
                    return ClassesDefinitions.LogRecord.newBuilder()
                            .setId(lr.getReplicaManagerId())
                            .putAllTimestamp(lr.getTimestamp().getMap())
                            .setUpdate(ClassesDefinitions.Update.newBuilder()
                                    .setOperationName(lr.getUpdate().getOperationName())
                                    .addAllArguments(lr.getUpdate().getOperationArgs())
                                    .putAllTimestamp(lr.getUpdate().getTimestamp().getMap()).build())
                            .setPhysicalClock(lr.getPhysicalClock())
                            .setStatus(status)
                            .build();
                }).collect(Collectors.toList());

        DebugMessage.debug("Propagating log records:\n"
                + logReport.getLogRecords().stream().map(q -> q + "\n").toList(), null, DEBUG_FLAG);

        for (ClassServerNamingServer.ServerAddress se : servers) {
            if (se.getHost().equals(replicaManager.getHost()) && se.getPort() == replicaManager.getPort()) {
                continue;
            }
            DebugMessage.debug("Propagating to secondary server @ " + se.getHost() + ":" + se.getPort() + "...",
                    null, DEBUG_FLAG);

            ClassServerClassServer.PropagateStateRequest propagateStateRequest = ClassServerClassServer
                    .PropagateStateRequest.newBuilder()
                    .addAllLogRecords(logRecords)
                    .putAllWriteTimestamp(logReport.getTimestamp().getMap())
                    .setIssuer(logReport.getIssuer())
                    .build();
            ClassServerClassServer.PropagateStateResponse response;

            try {

                ManagedChannel serverChannel = ManagedChannelBuilder.forAddress(se.getHost(), se.getPort())
                        .usePlaintext().build();
                ClassServerServiceGrpc.ClassServerServiceBlockingStub serverStub = ClassServerServiceGrpc.newBlockingStub(serverChannel);
                response = serverStub.withDeadlineAfter(5, TimeUnit.SECONDS).propagateState(propagateStateRequest);

                ResponseCode code = response.getCode();
                //DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
                serverChannel.shutdown();

            } catch (StatusRuntimeException e) {

                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) { // The backup server performed a peer shutdown
                    DebugMessage.debug("No other servers available to propagate state.", null, DEBUG_FLAG);
                    responseObserver.onError(INVALID_ARGUMENT
                            .withDescription("No other servers available to propagate state.")
                        .asRuntimeException());

                } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    DebugMessage.debug("Timeout on the requested operation.", null, DEBUG_FLAG);
                    responseObserver.onError(INVALID_ARGUMENT
                            .withDescription("Timeout on the requested operation.")
                            .asRuntimeException());
                } else {
                    responseObserver.onError(INVALID_ARGUMENT
                            .withDescription(e.getStatus().getDescription())
                            .asRuntimeException());
                }
            }
        }

        GossipResponse response = GossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
