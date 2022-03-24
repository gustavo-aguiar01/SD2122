package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Scanner;

public class Professor {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 8080;

  private static final String OPEN_ENROLLMENTS_CMD = "openEnrollments";
  private static final String CLOSE_ENROLLMENTS_CMD = "closeEnrollments";
  private static final String LIST_CMD = "list";
  private static final String CANCEL_ENROLLMENT_CMD = "cancelEnrollment";
  private static final String EXIT_CMD = "exit";

  public static void main(String[] args) {

    // Argument validation
    if (args.length != 0) {
      ErrorMessage.fatalError("No arguments needed!");
    }

    // Frontend connection establishment
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(HOSTNAME, PORT_NUMBER).usePlaintext().build();
    ProfessorFrontend frontend = new ProfessorFrontend(channel);
    Scanner scanner = new Scanner(System.in);

    while(true) {
      System.out.printf("> ");
      String[] line = scanner.nextLine().split(" ");

      // Open enrollments - openEnrollments cmd
      if (OPEN_ENROLLMENTS_CMD.equals(line[0])) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " +  OPEN_ENROLLMENTS_CMD + "command usage.");
          continue;
        }
        try {
          int capacity = Integer.parseInt(line[1]);
          System.out.println(frontend.openEnrollments(capacity));
        } catch (NumberFormatException e) {
          ErrorMessage.error(line[1] + " is not a valid integer!");
        } catch (StatusRuntimeException e) {
          ErrorMessage.error(e.getStatus().getDescription());
        }
      }

      // Close enrollments - closeEnrollments cmd
      if (CLOSE_ENROLLMENTS_CMD.equals(line[0])) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid" + CLOSE_ENROLLMENTS_CMD + "command usage.");
          continue;
        }
        System.out.println(frontend.closeEnrollments());
      }

      // List - list cmd
      if (LIST_CMD.equals(line[0])) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid" + LIST_CMD + "command usage.");
          continue;
        }
        System.out.println(frontend.listClass());
      }

      // Cancel enrollment - cancelEnrollment cmd
      if (CANCEL_ENROLLMENT_CMD.equals(line[0])) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid" + CANCEL_ENROLLMENT_CMD + "command usage.");
          continue;
        }
        try {
          System.out.println(frontend.cancelEnrollment(line[1]));
        } catch (StatusRuntimeException e) {

          ErrorMessage.error(e.getStatus().getDescription());
        }
      }

      // Local command to terminate - exit cmd
      if (EXIT_CMD.equals(line[0])) {
        break;
      }

      System.out.printf("%n");
    }
    channel.shutdown();
  }
}
