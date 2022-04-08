package pt.ulisboa.tecnico.classes.admin;

import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Scanner;
import java.util.Arrays;

public class Admin {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 5000;
  private static final String SERVICE_NAME = "Turmas";

  private static final String EXIT_CMD = "exit";
  private static final String ACTIVATE_CMD = "activate";
  private static final String DEACTIVATE_CMD = "deactivate";
  private static final String DUMP_CMD = "dump";

  /**
   * Admin class main functionality
   *  - Parse arguments
   *  - Make remote calls
   * @param args
   */
  public static void main(String[] args) {

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

    AdminFrontend adminFrontend = null; // Either it's assigned or has fatal error - never null.
    try {
      adminFrontend = new AdminFrontend(HOSTNAME, PORT_NUMBER, SERVICE_NAME);
    } catch (RuntimeException e) { // Case where there are no servers available - abort execution.
      ErrorMessage.fatalError(e.getMessage());
    }
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.printf("> ");

      // Split input by spaces to obtain command and args
      String[] line = scanner.nextLine().split(" ");
      String command = line[0];
      String[] arguments = Arrays.copyOfRange(line, 1, line.length);

      // Activate a server - activate cmd
      if (ACTIVATE_CMD.equals(command)) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " +  ACTIVATE_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(adminFrontend.activate(arguments[0]));
        } catch (RuntimeException e) {
          System.out.println(e.getMessage());
        }
      }

      // Deactivate a server - deactivate cmd
      else if (DEACTIVATE_CMD.equals(command)) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " +  DEACTIVATE_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(adminFrontend.deactivate(arguments[0]));
        } catch (RuntimeException e){
          throw new RuntimeException(e.getMessage());
        }
      }

      // Dump server class state - dump cmd
      else if (DUMP_CMD.equals(command)) {
        if (line.length != 2) {
          ErrorMessage.error("Invalid " +  DUMP_CMD + " command usage.");
          continue;
        }
        try {
          System.out.println(adminFrontend.dump(arguments[0]));
        } catch (RuntimeException e) {
          throw new RuntimeException(e.getMessage());
        }
      }

      // Terminate program
      else if (EXIT_CMD.equals(command)) {
        if (line.length != 1) {
          ErrorMessage.error("Invalid " +  EXIT_CMD + " command usage.");
          continue;
        }
        adminFrontend.shutdown();
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
