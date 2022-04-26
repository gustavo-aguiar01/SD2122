package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.ClassServer;
import pt.ulisboa.tecnico.classes.classserver.LogRecord;
import pt.ulisboa.tecnico.classes.classserver.ReplicaManager;
import pt.ulisboa.tecnico.classes.classserver.StateUpdate;
import pt.ulisboa.tecnico.classes.classserver.domain.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;

import java.time.Instant;
import java.util.stream.Collectors;

public class ClassServerServiceImpl extends ClassServerServiceImplBase {

    ReplicaManager replicaManager;

    public ClassServerServiceImpl(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        ResponseCode code;
        PropagateStateResponse response;

        try {
            replicaManager.integrateLogRecords(request.getLogRecordsList()
                    .stream().map(lr -> {
                                LogRecord.Status status = LogRecord.Status.SUCCESS;
                                switch (lr.getStatus()) {
                                    case SUCCESS -> status = LogRecord.Status.SUCCESS;
                                    case FAIL -> status = LogRecord.Status.FAIL;
                                    case NONE -> status = LogRecord.Status.NONE;
                                }
                                return new LogRecord(lr.getId(),
                                        new Timestamp(lr.getTimestampMap()),
                                        new StateUpdate(lr.getUpdate().getOperationName(),
                                                lr.getUpdate().getArgumentsList(),
                                                new Timestamp(lr.getUpdate().getTimestampMap())),
                                        lr.getPhysicalClock(), status);
                            })
                    .collect(Collectors.toList()),
                    new Timestamp(request.getWriteTimestampMap()),
                    request.getIssuer(), false);
            code = ResponseCode.OK;

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        }

        response = PropagateStateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
