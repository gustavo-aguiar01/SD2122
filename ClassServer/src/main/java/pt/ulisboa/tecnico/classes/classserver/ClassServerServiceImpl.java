package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.classserver.Class;
import pt.ulisboa.tecnico.classes.classserver.ClassServer;
import pt.ulisboa.tecnico.classes.classserver.ClassStudent;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;

import java.util.stream.Collectors;

public class ClassServerServiceImpl extends ClassServerServiceImplBase {

    ClassServer.ClassServerState serverState;

    public ClassServerServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        ResponseCode code;
        PropagateStateResponse response;
        ClassState state = request.getClassState();
        try {
            Class studentClass = serverState.getStudentClass(false);
            studentClass.setCapacity(state.getCapacity());
            studentClass.setRegistrationsOpen(state.getOpenEnrollments());
            studentClass.setEnrolledStudents(state.getEnrolledList().stream().map(s -> new ClassStudent(s.getStudentId(), s.getStudentName())).collect(Collectors.toList()));
            studentClass.setDiscardedStudents(state.getDiscardedList().stream().map(s -> new ClassStudent(s.getStudentId(), s.getStudentName())).collect(Collectors.toList()));
            code = ResponseCode.OK;
        } catch (InactiveServerException e) {
            code = ResponseCode.INACTIVE_SERVER;
        }
        response = PropagateStateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
