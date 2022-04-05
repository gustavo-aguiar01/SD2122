package pt.ulisboa.tecnico.classes.classserver.implementations;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.classserver.*;
import pt.ulisboa.tecnico.classes.classserver.Class;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;

import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc.AdminServiceImplBase;

public class AdminServiceImpl extends AdminServiceImplBase {
    ClassServer.ClassServerState serverState;

    public AdminServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    /**
     * "activate" remote call server implementation
     * @param request
     * @param responseObserver
     */
    @Override
    public void activate (ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        serverState.setActive(true);
        ActivateResponse response = ActivateResponse.newBuilder().build();

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

        serverState.setActive(false);
        DeactivateResponse response = DeactivateResponse.newBuilder().build();

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
            Class studentClass = serverState.getStudentClass(true);

            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getEnrolledStudentsCollection()))
                    .addAllEnrolled(ClassUtilities.classStudentsToGrpc(studentClass.getRevokedStudentsCollection()))
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
}
