package pt.ulisboa.tecnico.classes.admin;

import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Scanner;
import java.util.Arrays;


public class Admin {

  private static final String HOSTNAME = "localhost";
  private static final int PORT_NUMBER = 5000;

  private static final String SERVICE = "Turmas";

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
        ErrorMessage.fatalError("Invalid argument passed, try -debug");
      }
    }

    AdminFrontend frontend = new AdminFrontend(HOSTNAME, PORT_NUMBER, SERVICE);
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.printf("> ");

      // split input by spaces to obtain command and args
      String[] line = scanner.nextLine().split(" ");
      String command = line[0];
      String[] arguments = Arrays.copyOfRange(line, 1, line.length);

      String response;
      if (EXIT_CMD.equals(command)) {
        frontend.shutdown();
        scanner.close();
        System.exit(0);
      }
      else if (ACTIVATE_CMD.equals(command)) {
        try {
          response = frontend.activate();
          System.out.println(response);
        } catch (RuntimeException e) {
          System.out.println(e.getMessage());
        }
      }
      else if (DEACTIVATE_CMD.equals(command)) {
        try {
          response = frontend.deactivate();
          System.out.println(response);
        } catch (RuntimeException e){
          throw new RuntimeException(e.getMessage());
        }
      }
      else if (DUMP_CMD.equals(command)) {
        try {
          response = frontend.dump();
          System.out.println(response);
        } catch (RuntimeException e){
          throw new RuntimeException(e.getMessage());
        }
      } else {
        System.out.println("Command not found.");
      }
      System.out.printf("%n");
    }

  }
}
