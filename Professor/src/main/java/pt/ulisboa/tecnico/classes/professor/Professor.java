package pt.ulisboa.tecnico.classes.professor;

import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Arrays;
import java.util.Scanner;

public class Professor {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 5000;
  private static final String SERVICE = "Turmas";

  private static final String OPEN_ENROLLMENTS_CMD = "openEnrollments";
  private static final String CLOSE_ENROLLMENTS_CMD = "closeEnrollments";
  private static final String LIST_CMD = "list";
  private static final String CANCEL_ENROLLMENT_CMD = "cancelEnrollment";
  private static final String EXIT_CMD = "exit";

  /**
   * Professor class main functionality
   *  - Parse arguments
   *  - Make remote calls
   * @param args
   */
  public static void main(String[] args) {

    // Argument validation
    if (args.length > 1) {
      ErrorMessage.fatalError("No arguments needed.");
    }

    if (args.length == 1) {
      if (args[0].equals("-debug")) {
        System.setProperty("debug", "true");
      } else {
        ErrorMessage.fatalError("Invalid argument passed, try -debug.");
      }
    }

    ProfessorFrontend professorFrontend = null; // Either it's assigned or has fatal error - never null.
    try {
      professorFrontend= new ProfessorFrontend(HOSTNAME, PORT_NUMBER, SERVICE);
    } catch (RuntimeException e) { // Case where there are no servers available - abort execution.
      ErrorMessage.fatalError(e.getMessage());
    }
    Scanner scanner = new Scanner(System.in);

    while(true) {
      System.out.printf("> ");

      // Split input by spaces to obtain command and args
      String[] line = scanner.nextLine().split(" ");
      String command = line[0];
      String[] arguments = Arrays.copyOfRange(line, 1, line.length);

      // Open class enrollments - openEnrollments cmd
      if (OPEN_ENROLLMENTS_CMD.equals(command)) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " +  OPEN_ENROLLMENTS_CMD + " command usage.");
          continue;
        }
        try {
          int capacity = Integer.parseInt(arguments[0]);
          System.out.println(professorFrontend.openEnrollments(capacity));
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      // Close class enrollments - closeEnrollments cmd
      else if (CLOSE_ENROLLMENTS_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " + CLOSE_ENROLLMENTS_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(professorFrontend.closeEnrollments());
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }

      }

      // List class state - list cmd
      else if (LIST_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " + LIST_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(professorFrontend.listClass());
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      // Cancel enrollment - cancelEnrollment cmd
      else if (CANCEL_ENROLLMENT_CMD.equals(command)) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " + CANCEL_ENROLLMENT_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(professorFrontend.cancelEnrollment(arguments[0]));
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      // Terminate program
      else if (EXIT_CMD.equals(command)) {
        professorFrontend.shutdown();
        scanner.close();
        System.exit(0);
      }

      // Invalid command given
      else {
        System.out.println("Command not found.");
      }

      System.out.printf("%n");
    }
  }
}
