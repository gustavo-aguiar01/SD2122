package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;


import pt.ulisboa.tecnico.classes.DebugMessage;
import pt.ulisboa.tecnico.classes.ErrorMessage;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;
import pt.ulisboa.tecnico.classes.classserver.implementations.*;

public class ClassServer {

  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  // Class service name
  private static final String CLASS_SERVICE_NAME = "Turmas";

  // Naming Server info
  private static final String NAMING_HOSTNAME = "localhost";
  private static final int NAMING_PORT_NUMBER = 5000;

  // Server host port
  private static int port;
  private static String host;
  private static String primary;

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
    System.out.printf("Received %d Argument(s).%n", args.length);

    if (args.length < 3 || args.length > 4) {
      ErrorMessage.fatalError("Invalid arguments expected : <hostname> <port> <P/S> [-debug].");
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

    if (! (args[2].equals("P") || args[2].equals("S"))) {
      ErrorMessage.fatalError("Invalid arguments expected : <hostname> <port> <P/S> [-debug].");
    }

    primary = args[2];

    if (args.length == 4) {
      if (args[3].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug.");
      }
    }

    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    ReplicaManager replicaManager = new ReplicaManager(primary, host, port);

    // Create services instances
    final BindableService adminImpl = new AdminServiceImpl(replicaManager);
    final BindableService professorImpl = new ProfessorServiceImpl(replicaManager);
    final BindableService studentImpl = new StudentServiceImpl(replicaManager);
    final BindableService classImpl = new ClassServerServiceImpl(replicaManager);

    Server server = ServerBuilder.forPort(port).addService(adminImpl)
              .addService(professorImpl).addService(studentImpl).addService(classImpl).build();

    final ClassFrontend classFrontend = new ClassFrontend(replicaManager, NAMING_HOSTNAME, NAMING_PORT_NUMBER);

    try {

      classFrontend.register(CLASS_SERVICE_NAME, host, port, primary);

    } catch (RuntimeException e) {
      ErrorMessage.fatalError(e.getMessage());
    }

    // Class that allows a primary server to repeatedly propagate its state
    class PropagateState extends TimerTask {
      @Override
      public void run() {
        try {
          DebugMessage.debug(classFrontend
                  .propagateState(CLASS_SERVICE_NAME)
                  , "propagateState", DEBUG_FLAG);
        } catch (InactiveServerException e) {
          ErrorMessage.error("Primary server tried to propagate its state while being inactive.");
        } catch (RuntimeException e) {
          ErrorMessage.fatalError("Failed to propagate primary server.");
        }
      }
    }

    // Every 10 seconds a primary server propagates its state to all secondary servers
    Timer timer;
    TimerTask task = new PropagateState();
    if (replicaManager.isPrimary()) {
      timer = new Timer();
      timer.schedule(task, 0, 10000);
    }

    // Make sure to delete the server from naming server upon termination
    class DeleteFromNamingServer extends Thread {
      public void run() {
        if (replicaManager.isPrimary()) { task.cancel(); }
        try {

          classFrontend.delete(CLASS_SERVICE_NAME, host, port);
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
