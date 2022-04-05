package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.namingserver.implementations.ClassServerServiceImpl;

public class NamingServer {

  /* Server host port. */
  private static int port = 5000;
  private static String host = "localhost";

  public static void main(String[] args) throws IOException, InterruptedException {

    System.out.println(NamingServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);

    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    if (args.length > 1) {
      ErrorMessage.fatalError("No arguments needed.");
    }

    if (args.length == 1) {
      if (args[0].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug");
      }
    }

    final BindableService serverImpl = new ClassServerServiceImpl();

    // Create a new server to listen on port.
    Server server = ServerBuilder.forPort(port).addService(serverImpl).build();

    server.start();
    server.awaitTermination();

  }
}
