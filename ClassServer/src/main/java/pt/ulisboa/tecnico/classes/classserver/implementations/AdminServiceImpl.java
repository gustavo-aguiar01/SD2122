package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.classserver.*;
import pt.ulisboa.tecnico.classes.classserver.ClassStateReport;
import pt.ulisboa.tecnico.classes.classserver.domain.ClassUtilities;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;

import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc.AdminServiceImplBase;


public class AdminServiceImpl extends AdminServiceImplBase {

    ReplicaManager replicaManager;

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

        System.out.printf("forcing gossip\n");
        GossipResponse response = GossipResponse.getDefaultInstance();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
