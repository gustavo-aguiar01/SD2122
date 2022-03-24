package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;


/* Representation of the server state */
public class ClassServer {

  /* Server host port. */
  private static int port;
  private static String host;
  private static ClassServerState serverState;

  /* Server state class */
  public static class ClassServerState {

    private boolean active;
    private Class studentClass;
    private boolean primary;

    public ClassServerState (String primary) {
      this.active = true;
      this.primary = primary == "P";
      this.studentClass = new Class();
    }

    public Class getStudentClass() throws InactiveServerException {

      /* Can only access server contents if the server is active */
      if (! this.isActive()) {
        throw new InactiveServerException();
      }

      return studentClass;
    }

    public synchronized void setActive(boolean active) {
      this.active = active;
    }
    public synchronized boolean isActive() {
      return active;
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    System.out.println(ClassServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);

    if (args.length != 3) {
      ErrorMessage.errorExit("Invalid command expected : <hostname> <port> <P/S>");
    }

    host = args[0];

    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      ErrorMessage.errorExit("Invalid port number");
    }

    if (1024 <= port && port <= 65535) {
      ErrorMessage.errorExit("Invalid port number");
    }

    if (! (args[2] == "P" || args[2] == "S")) {
      ErrorMessage.errorExit("Invalid command expected : <hostname> <port> <P/S>");
    }

    serverState = new ClassServerState(args[2]);


    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    // create services instances
    final BindableService adminImpl = new AdminServiceImpl(serverState);
    final BindableService professorImpl = new ProfessorServiceImpl(serverState);
    final BindableService studentImpl = new StudentServiceImpl(serverState);

    // Create a new server to listen on port.
    Server server = ServerBuilder.forPort(port).addService(adminImpl)
            .addService(professorImpl).addService(studentImpl).build();

    server.start();
    server.awaitTermination();

  }
}
