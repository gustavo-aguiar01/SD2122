package pt.ulisboa.tecnico.classes.namingserver.implementations;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.naming.ClassNamingServerServiceGrpc.ClassNamingServerServiceImplBase;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.namingserver.domain.NamingServices;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.AlreadyExistingPrimaryServerException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.AlreadyExistingServerException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.InvalidServerInfoException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ClassServerServiceImpl extends ClassNamingServerServiceImplBase {

    NamingServices services = new NamingServices();

    /**
     * "register" server remote call facade
     *      adds a new server to the name server records
     * @param request
     * @param responseObserver
     */
    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        Map<String, String> qualifiers = new HashMap<>();
        request.getQualifiersList().forEach(q -> qualifiers.put(q.getName(), q.getValue()));

        try {

            services.addService(request.getServiceName(), request.getHost(), request.getPort(), qualifiers);

            responseObserver.onNext(RegisterResponse.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (InvalidServerInfoException e) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Invalid server info: server port should be a legal port and/or \"primaryStatus\" " +
                            "should be a qualifier with value \"P\" or \"S\".")
                    .asRuntimeException());
        } catch (AlreadyExistingServerException e) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("A server associated with the service " + request.getServiceName()
                            + " with the given host:port already exists.")
                    .asRuntimeException());
        } catch (AlreadyExistingPrimaryServerException e) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("A primary server associated with the service " + request.getServiceName()
                            + " already exists.")
                    .asRuntimeException());
        }

    }

    /**
     * "lookup" server remote call facade
     *      returns a list of servers associated with a
     *      given service and with some given attributes
     * @param request
     * @param responseObserver
     */
    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {

        String serviceName = request.getServiceName();
        HashMap<String, String> qualifiers = new HashMap<>();
        request.getQualifiersList().forEach(q -> qualifiers.put(q.getName(), q.getValue()));

        List<ServerAddress> servers;
        servers = services.lookupServersOfService(serviceName, qualifiers).stream()
                .map(s -> ServerAddress.newBuilder().setHost(s.getHost()).setPort(s.getPort()).build())
                .collect(Collectors.toList());

        LookupResponse response = LookupResponse.newBuilder().addAllServers(servers).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    /**
     * "delete" server remote call facade
     *      removes an entry from the naming server
     *      records
     * @param request
     * @param responseObserver
     */
    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {

        // We allow a request to have a service name that doesn't exist - nothing is done
        services.deleteService(request.getServiceName(), request.getHost(), request.getPort());

        responseObserver.onNext(DeleteResponse.getDefaultInstance());
        responseObserver.onCompleted();

    }
}
