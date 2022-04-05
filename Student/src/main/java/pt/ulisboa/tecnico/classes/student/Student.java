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

    Scanner scanner = new Scanner(System.in);
    if (args.length < 2) {
      ErrorMessage.fatalError("Invalid command expected : alunoXXXX <nome>*, where XXXX is 4 digit positive number" );
    }

    int argsLength = args.length;
    if (args[argsLength - 1].equals("-debug")) {
      System.setProperty("debug", "true");
      argsLength--;

      /* Check if name and id where introduced and not just id + debug flag */
      if (args.length < 3) {
        ErrorMessage.fatalError("Invalid command expected : alunoXXXX <nome>* -debug, where XXXX is 4 digit positive number" );
      }
    }

    final String id = args[0];

    StringBuilder nameBuilder = new StringBuilder(args[1]);
    for (int i = 2; i < argsLength; i++) {
      nameBuilder.append(" " + args[i]);
    }
    final String name = nameBuilder.toString();

    System.out.printf("Creating frontend\n");
    final StudentFrontend studentFrontend = new StudentFrontend(HOSTNAME, PORT_NUMBER, SERVICE);
    System.out.printf("Frontend created\n");
    while (true) {
      System.out.printf("> ");
      String line = scanner.nextLine();

      if (ENROLL_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.enroll(id, name));
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      if (LIST_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.listClass());
        } catch (RuntimeException e) {
          ErrorMessage.error(e.getMessage());
          System.exit(1);
        }
      }

      if (EXIT_CMD.equals(line)) {
        studentFrontend.shutdown();
        scanner.close();
        System.exit(0);
      }

      System.out.printf("%n");
    }
  }
}
