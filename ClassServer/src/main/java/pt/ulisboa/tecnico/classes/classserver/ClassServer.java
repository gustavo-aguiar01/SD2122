package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;


import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.classserver.domain.Class;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InvalidOperationException;
import pt.ulisboa.tecnico.classes.classserver.implementations.*;

public class ClassServer {

  /* Naming Server info */
  private static final String NAMING_HOSTNAME = "localhost";
  private static final int NAMING_PORT_NUMBER = 5000;

  /* Server host port. */
  private static int port;
  private static String host;
  private static ClassServerState serverState;
  private static String primary;

  /* Server state class */
  public static class ClassServerState {

    public enum ACCESS_TYPE {
      READ,
      WRITE
    }

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

    public Class getStudentClassToWrite(boolean isAdmin) throws InactiveServerException,
            InvalidOperationException{
      if (!primary) {
        DebugMessage.debug("Cannot execute write operation on backup server", null, DEBUG_FLAG);
        throw new InvalidOperationException();
      }
      return this.getStudentClass(isAdmin);

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

    primary = args[2];

    if (args.length == 4) {
      if (args[3].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug");
      }
    }

    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    serverState = new ClassServerState(primary);

    // Create services instances
    final BindableService adminImpl = new AdminServiceImpl(serverState);
    final BindableService professorImpl = new ProfessorServiceImpl(serverState);
    final BindableService studentImpl = new StudentServiceImpl(serverState);
    final BindableService classImpl = new ClassServerServiceImpl(serverState);

    Server server = ServerBuilder.forPort(port).addService(adminImpl)
              .addService(professorImpl).addService(studentImpl).addService(classImpl).build();

    final ClassFrontend classFrontend = new ClassFrontend(NAMING_HOSTNAME, NAMING_PORT_NUMBER);

    try {
      classFrontend.register("Turmas", host, port, primary);
    } catch (RuntimeException e) {
      ErrorMessage.fatalError(e.getMessage());
      System.exit(1);
    }

    // Class that allows the primary server to repeatedly propagate its state
    class PropagateState extends TimerTask {
      @Override
      public void run() {
        try {
          DebugMessage.debug(classFrontend.propagateState(serverState.getStudentClass(false).reportClassState()), "propagateState", ClassServerState.DEBUG_FLAG);
        } catch (InactiveServerException e) {
          ErrorMessage.error("Primary server tried to propagate its state while being inactive.");
        } catch (RuntimeException e) {
          ErrorMessage.fatalError("Failed to propagate primary server.");
        }
      }
    }

    // Every second the primary server propagates its state to the secondary server
    Timer timer;
    TimerTask task = new PropagateState();
    if (serverState.primary) {
      timer = new Timer();
      timer.schedule(task, 0, 5000);
    }

    // Make sure to delete the server from naming server upon termination
    class DeleteFromNamingServer extends Thread {
      public void run() {
        task.cancel();
        try {
          classFrontend.delete("Turmas", host, port);
          classFrontend.shutdown();
          server.shutdownNow();
        } catch (RuntimeException e) {
          classFrontend.shutdown();
          ErrorMessage.error(e.getMessage());
          Runtime.getRuntime().halt(-1);
        }
      }
    }

    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new DeleteFromNamingServer());

    server.start();
    server.awaitTermination();
  }
}
