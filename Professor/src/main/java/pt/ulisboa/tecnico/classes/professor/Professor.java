package pt.ulisboa.tecnico.classes.professor;

import java.util.Scanner;

public class Professor {

  private static final String OPEN_ENROLLMENTS_CMD = "abrir_inscricoes";
  private static final String CLOSE_ENROLLMENTS_CMD = "fechar_inscricoes";
  private static final String CANCEL_ENROLLMENTS_CMD = "cancelar_inscricao";

  public static void main(String[] args) {
    System.out.println(Professor.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    // Argument validation
    if (args.length != 0) {
      System.err.println("No arguments needed!");
      System.err.printf("Usage: java %s%n", Professor.class.getSimpleName());
    }

    // Frontend connection establishment
    ProfessorFrontend frontend = new ProfessorFrontend();
    Scanner scanner = new Scanner(System.in);

    while(true) {
      System.out.printf("%n> ");
      String[] line = scanner.nextLine().split(" ");

      // Open enrollments - abrir_inscricoes cmd
      if (OPEN_ENROLLMENTS_CMD.equals(line[0])) {
        try {
          int capacity = Integer.parseInt(line[1]);
          System.out.println(frontend.openEnrollments(capacity));
        } catch (NumberFormatException e) {
          System.err.println("ERROR: " + line[1] + " is not a valid integer!");
        }
      }

      // Close enrollments - fechar_inscricoes cmd
      if (CLOSE_ENROLLMENTS_CMD.equals(line[0])) {
        System.out.println(frontend.closeEnrollments());
      }

      // Open enrollments - abrir_inscricoes cmd
      if (CANCEL_ENROLLMENTS_CMD.equals(line[0])) {
        // TODO: Add verifications
        System.out.println(frontend.cancelEnrollment(line[1]));
      }

    }


  }
}
