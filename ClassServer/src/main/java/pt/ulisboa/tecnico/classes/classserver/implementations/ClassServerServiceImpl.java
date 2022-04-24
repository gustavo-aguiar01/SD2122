package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.Timestamp;
import pt.ulisboa.tecnico.classes.classserver.ClassServer;
import pt.ulisboa.tecnico.classes.classserver.ReplicaManager;
import pt.ulisboa.tecnico.classes.classserver.domain.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;

public class ClassServerServiceImpl extends ClassServerServiceImplBase {

    ReplicaManager replicaManager;

    public ClassServerServiceImpl(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        ResponseCode code;
        PropagateStateResponse response;
        ClassState state = request.getClassState();

        try {
            replicaManager.setClassState(state.getCapacity(), state.getOpenEnrollments(),
                            ClassUtilities.studentsToDomain(state.getEnrolledList()),
                            ClassUtilities.studentsToDomain(state.getDiscardedList()),
                            new Timestamp(request.getTimestampMap()), false);
            code = ResponseCode.OK;

        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        }

        response = PropagateStateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
