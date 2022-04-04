package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc.*;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassFrontend {

    private final ClassNamingServerServiceBlockingStub namingStub;

    /* Set flag to true to print debug messages. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public ClassFrontend(String hostname, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        namingStub = ClassNamingServerServiceGrpc.newBlockingStub(channel);
    }

    public void register(String serviceName, String host, int port, String primary) throws RuntimeException {

        List<Qualifier> qualifiers = new ArrayList<Qualifier>();
        qualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue(primary).build());

        DebugMessage.debug("Calling register remote call", "register", DEBUG_FLAG);

        try {       
        namingStub.register(RegisterRequest.newBuilder().setServiceName(serviceName)
                    .setHost(host).setPort(port).addAllQualifiers(qualifiers).build());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getStatus().getDescription());
        }
    }

    public void delete(String serviceName, String host, int port) {
        DebugMessage.debug("Calling delete remote call", "delete", DEBUG_FLAG);
        namingStub.delete(DeleteRequest.newBuilder().setServiceName(serviceName)
                .setHost(host).setPort(port).build());
    }

    public String propagateState(Class studentClass) {
        DebugMessage.debug("Calling propagateState remote call", "propagateState", DEBUG_FLAG);
        List<Qualifier> qualifiers = new ArrayList<>();
        qualifiers.add(Qualifier.newBuilder().setName("primaryStatus").setValue("S").build());
        List<ServerAddress> servers = namingStub.lookup(LookupRequest.newBuilder()
                .setServiceName("Turmas").addAllQualifiers(qualifiers).build()).getServersList();
        if (servers.size() == 0) {
            DebugMessage.debug("No secondary servers available!", null, DEBUG_FLAG);
        }
        String message = Stringify.format(ResponseCode.OK);
        for (ServerAddress se : servers) {
            DebugMessage.debug("Propagating to secondary server @ " + se.getHost() + ":" + se.getPort(), null, DEBUG_FLAG);
            ManagedChannel serverChannel = ManagedChannelBuilder.forAddress(se.getHost(), se.getPort()).usePlaintext().build();
            ClassServerServiceBlockingStub serverStub = ClassServerServiceGrpc.newBlockingStub(serverChannel);
            List<Student> enrolledStudents = studentClass.getEnrolledStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());
            List<Student> discardedStudents = studentClass.getRevokedStudentsCollection().stream()
                    .map(s -> Student.newBuilder().setStudentId(s.getId())
                            .setStudentName(s.getName()).build()).collect(Collectors.toList());
            ClassState state = ClassState.newBuilder().setCapacity(studentClass.getCapacity())
                    .setOpenEnrollments(studentClass.areRegistrationsOpen())
                    .addAllEnrolled(enrolledStudents).addAllDiscarded(discardedStudents).build();
            PropagateStateResponse response = serverStub.propagateState(PropagateStateRequest.newBuilder().setClassState(state).build());
            ResponseCode code = response.getCode();
            message = Stringify.format(code);
            DebugMessage.debug("Got the following response: " + message, null, DEBUG_FLAG);
            if (code == ResponseCode.OK) {
                DebugMessage.debug("Primary server propagated successfully", null, DEBUG_FLAG);
            } else {
                DebugMessage.debug("Primary server failed to propagate with code: " + code, null, DEBUG_FLAG);
            }
        }
        return message;
    }
}
