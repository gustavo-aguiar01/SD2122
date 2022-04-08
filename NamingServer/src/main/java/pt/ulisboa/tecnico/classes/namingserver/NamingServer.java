package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.namingserver.implementations.ClassServerServiceImpl;

public class NamingServer {

  // Naming Server's hostname and port number
  private static int port;
  private static String host;

  /**
   * Naming server class main functionality
   * @param args
   * @throws IOException
   * @throws InterruptedException
   */
  public static void main(String[] args) throws IOException, InterruptedException {

    System.out.println(NamingServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s).%n", args.length);

    int argsLength = args.length;

    if (argsLength < 2 || argsLength > 3) {
      ErrorMessage.fatalError("Invalid arguments expected : <hostname> <port> [-debug].");
    }

    host = args[0];

    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      ErrorMessage.fatalError("Invalid port number.");
    }

    if (!(1024 <= port && port <= 65535)) {
      ErrorMessage.fatalError("Invalid port number.");
    }

    if (args.length == 3) {
      if (args[2].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug.");
      }
    }

    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    final BindableService serverImpl = new ClassServerServiceImpl();

    // Create a new server to listen on port.
    Server server = ServerBuilder.forPort(port).addService(serverImpl).build();

    server.start();
    server.awaitTermination();

  }
}
