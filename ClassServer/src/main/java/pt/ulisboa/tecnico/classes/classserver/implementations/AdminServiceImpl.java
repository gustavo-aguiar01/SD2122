package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;

import java.util.stream.Collectors;
import java.util.List;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
    ClassServer.ClassServerState serverState;

    public AdminServiceImpl(ClassServer.ClassServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void activate (ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        serverState.setActive(true);
        ActivateResponse response = ActivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate (DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {

        serverState.setActive(false);
        DeactivateResponse response = DeactivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void dump (DumpRequest request, StreamObserver<DumpResponse> responseObserver) {

        DumpResponse response;

        List<ClassesDefinitions.Student> enrolledStudents = serverState.getStudentClass().getEnrolledStudentsCollection().stream()
                .map(s -> ClassesDefinitions.Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        List<ClassesDefinitions.Student> discardedStudents = serverState.getStudentClass().getRevokedStudentsCollection().stream()
                .map(s -> ClassesDefinitions.Student.newBuilder().setStudentId(s.getId())
                        .setStudentName(s.getName()).build()).collect(Collectors.toList());

        ClassesDefinitions.ClassState state = ClassesDefinitions.ClassState.newBuilder().setCapacity(serverState.getStudentClass().getCapacity())
                .setOpenEnrollments(serverState.getStudentClass().areRegistrationsOpen())
                .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();

        response = DumpResponse.newBuilder().setCode(ClassesDefinitions.ResponseCode.OK)
                .setClassState(state).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
