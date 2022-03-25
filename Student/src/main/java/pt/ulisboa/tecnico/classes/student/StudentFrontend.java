package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;

public class StudentFrontend {

    private final StudentServiceGrpc.StudentServiceBlockingStub stub;
    private final ManagedChannel channel;

    public StudentFrontend(String hostname, int port) {
        channel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    public String enroll(String id, String name) throws RuntimeException {

        try {
            Student newStudent = Student.newBuilder().setStudentId(id).setStudentName(name).build();
            return Stringify.format(stub.enroll(EnrollRequest.newBuilder().setStudent(newStudent).build()).getCode());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                return e.getStatus().getDescription();
            } else {
                throw new RuntimeException(e.getStatus().getDescription());
            }
        }
    }

    public String listClass() throws RuntimeException {

        try {
            ListClassResponse response = stub.listClass(ListClassRequest.getDefaultInstance());
            if (response.getCode() != ClassesDefinitions.ResponseCode.OK) {
                return Stringify.format(response.getCode());
            } else {
                return Stringify.format(response.getClassState());
            }
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public void shutdown() {
        channel.shutdown();
    }

}
