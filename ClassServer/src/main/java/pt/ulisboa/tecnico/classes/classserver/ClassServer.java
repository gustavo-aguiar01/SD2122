package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import pt.ulisboa.tecnico.classes.classserver.exceptions.InactiveServerException;

public class ClassServer {

  /* Server host port. */
  private static int port;
  private static ClassServerState serverState;

  /* Server state class */
  public static class ClassServerState {

    private boolean active;
    private Class studentClass;

    public ClassServerState () {
      this.active = true;
      this.studentClass = new Class();
    }

    public Class getStudentClass() throws InactiveServerException {
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
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    serverState = new ClassServerState();
    port = 8080;

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
