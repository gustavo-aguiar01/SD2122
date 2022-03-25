package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;

public class ClassServer {

  /* Server host port. */
  private static int port;
  private static String host;
  private static ClassServerState serverState;

  /* Server state class */
  public static class ClassServerState {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private boolean active;
    private Class studentClass;
    private boolean primary;

    public ClassServerState (String primary) {
      this.active = true;
      this.primary = primary.equals("P");
      this.studentClass = new Class();
    }

    /**
     *  Student class getter, but only returns it if the server
     *  is active (for non admin users)
     * @param isAdmin
     * @return Class
     * @throws InactiveServerException
     */
    public Class getStudentClass(boolean isAdmin) throws InactiveServerException {

      DebugMessage.debug("Getting class, from" + (isAdmin ? " " : " non ") + "admin user", "getStudentClass", DEBUG_FLAG);
      DebugMessage.debug("The server is" + (isActive() ? " " : " not ") + "active", null, DEBUG_FLAG);

      /* Can only access server contents if the server is active */
      if (!isAdmin && !this.isActive()) {
        DebugMessage.debug("It's not possible to obtain student class", null, DEBUG_FLAG);
        throw new InactiveServerException();
      }

      DebugMessage.debug("Student class returned successfully", null, DEBUG_FLAG);
      return studentClass;
    }

    /**
     * Set server availability
     * @param active
     */
    public synchronized void setActive(boolean active) {

      DebugMessage.debug("Server is now " + (active ? "active" : "inactive"), "setActive", DEBUG_FLAG);
      this.active = active;

    }

    /**
     * Checks if the server is active
     * @return boolean
     */
    public synchronized boolean isActive() {
      return active;
    }
  }

  /**
   * Server main functionality
   *  - Parse arguments
   *  - Process and respond to remote calls
   * @param args
   * @throws IOException
   * @throws InterruptedException
   */
  public static void main(String[] args) throws IOException, InterruptedException {

    System.out.println(ClassServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);

    if (args.length < 3 || args.length > 4) {
      ErrorMessage.fatalError("Invalid command expected : <hostname> <port> <P/S>");
    }

    host = args[0];

    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      ErrorMessage.fatalError("Invalid port number");
    }

    if (!(1024 <= port && port <= 65535)) {
      ErrorMessage.fatalError("Invalid port number");
    }

    if (! (args[2].equals("P") || args[2].equals("S"))) {
      ErrorMessage.fatalError("Invalid command expected : <hostname> <port> <P/S>");
    }

    if (args.length == 4) {
      if (args[3].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug");
      }
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
