package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ClassServer {

  /* Server host port. */
  private static int port;

  public static void main(String[] args) throws IOException, InterruptedException {

    Class studentClass = new Class();

    System.out.println(ClassServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    port = Integer.valueOf(args[0]);

    final BindableService adminImpl = new AdminServiceImpl(studentClass);
    final BindableService professorImpl = new ProfessorServiceImpl(studentClass);
    final BindableService studentImpl = new StudentServiceImpl(studentClass);

    // Create a new server to listen on port.
    Server server = ServerBuilder.forPort(port).addService(adminImpl)
            .addService(professorImpl).addService(studentImpl).build();

    server.start();
    server.awaitTermination();

  }
}
