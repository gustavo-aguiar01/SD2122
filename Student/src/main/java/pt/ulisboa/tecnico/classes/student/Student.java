package pt.ulisboa.tecnico.classes.student;

import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Scanner;

public class Student {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 5000;
  private static final String SERVICE = "Turmas";

  private static final String ENROLL_CMD = "enroll";
  private static final String LIST_CMD = "list";
  private static final String EXIT_CMD = "exit";

  /**
   * Student class main functionality
   *  - Parse arguments
   *  - Make remote calls
   * @param args
   */
  public static void main(String[] args) {

    if (args.length < 2) {
      ErrorMessage.fatalError("Invalid command expected : alunoXXXX <nome>*, where XXXX is 4 digit positive number." );
    }

    int argsLength = args.length;
    if (args[argsLength - 1].equals("-debug")) {
      System.setProperty("debug", "true");
      argsLength--;

      // Check if name and id where introduced and not just id + debug flag
      if (args.length < 3) {
        ErrorMessage.fatalError("Invalid command expected : alunoXXXX <nome>* -debug, where XXXX is 4 digit positive number." );
      }
    }

    final String id = args[0];
    StringBuilder nameBuilder = new StringBuilder(args[1]);
    for (int i = 2; i < argsLength; i++) {
      nameBuilder.append(" ").append(args[i]);
    }
    final String name = nameBuilder.toString();

    StudentFrontend studentFrontend = null; // Either it's assigned or has fatal error - never null.
    try {
      studentFrontend = new StudentFrontend(HOSTNAME, PORT_NUMBER, SERVICE);
    } catch (RuntimeException e) { // Case where there are no servers available - abort execution.
      ErrorMessage.fatalError(e.getMessage());
    }
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.printf("> ");

      String[] line = scanner.nextLine().split(" ");
      String command = line[0];

      // Enroll student in class - enroll cmd
      if (ENROLL_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " +  ENROLL_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(studentFrontend.enroll(id, name));
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      // List server class state - list cmd
      else if (LIST_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " +  LIST_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(studentFrontend.listClass());
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      // Terminate program
      else if (EXIT_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " +  EXIT_CMD + " command usage.");
          continue;
        }
        studentFrontend.shutdown();
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
