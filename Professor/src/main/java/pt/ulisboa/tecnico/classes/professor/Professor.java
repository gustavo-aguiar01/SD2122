package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Scanner;

public class Professor {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 8080;

  private static final String OPEN_ENROLLMENTS_CMD = "openEnrollments";
  private static final String CLOSE_ENROLLMENTS_CMD = "closeEnrollments";
  private static final String LIST_CMD = "list";
  private static final String REVOKE_ENROLLMENT_CMD = "cancelEnrollment";
  private static final String EXIT_CMD = "exit";

  public static void main(String[] args) {

    // Argument validation
    if (args.length != 0) {
      System.err.println("No arguments needed!");
      System.err.printf("Usage: java %s%n", Professor.class.getSimpleName());
    }

    // Frontend connection establishment
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(HOSTNAME, PORT_NUMBER).usePlaintext().build();
    ProfessorFrontend frontend = new ProfessorFrontend(channel);
    Scanner scanner = new Scanner(System.in);

    while(true) {
      System.out.printf("> ");
      String[] line = scanner.nextLine().split(" ");

      // Open enrollments - open_enrollments cmd
      if (OPEN_ENROLLMENTS_CMD.equals(line[0])) {
        if (line.length != 2) {
          System.err.println("ERROR: Invalid open_enrollments command usage.");
          continue;
        }
        try {
          int capacity = Integer.parseInt(line[1]);
          System.out.println(frontend.openEnrollments(capacity));
        } catch (NumberFormatException e) {
          System.err.println("ERROR: " + line[1] + " is not a valid integer!");
        } catch (StatusRuntimeException e) {
          System.out.println("ERROR: " + e.getStatus().getDescription());
        }
      }

      // Close enrollments - close_enrollments cmd
      if (CLOSE_ENROLLMENTS_CMD.equals(line[0])) {
        if (line.length != 1) {
          System.err.println("ERROR: Invalid close_enrollments command usage.");
          continue;
        }
        System.out.println(frontend.closeEnrollments());
      }

      // List - list cmd
      if (LIST_CMD.equals(line[0])) {
        if (line.length != 1) {
          System.err.println("ERROR: Invalid list command usage.");
          continue;
        }
        System.out.println(frontend.listClass());
      }

      // Revoke enrollment - revoke_enrollment cmd
      if (REVOKE_ENROLLMENT_CMD.equals(line[0])) {
        if (line.length != 2) {
          System.err.println("ERROR: Invalid revoke_enrollment command usage.");
          continue;
        }
        try {
          System.out.println(frontend.cancelEnrollment(line[1]));
        } catch (StatusRuntimeException e) {
          System.out.println("ERROR: " + e.getStatus().getDescription());
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
