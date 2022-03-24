package pt.ulisboa.tecnico.classes.admin;

import java.util.Scanner;
import java.util.Arrays;

public class Admin {

  private static final String EXIT_CMD = "exit";
  private static final String ACTIVATE_CMD = "activate";
  private static final String DEACTIVATE_CMD = "deactivate";
  private static final String DUMP_CMD = "dump";

  public static void main(String[] args) {

    final String host = "localhost";
    final int port = 8080;

    AdminFrontend frontend = new AdminFrontend(host, port);

    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.printf("> ");

      // split input by spaces to obtain command and args
      String[] line = scanner.nextLine().split(" ");
      String command = line[0];
      String[] arguments = Arrays.copyOfRange(line, 1, line.length);

      String response;
      if (EXIT_CMD.equals(command)) {
        scanner.close();
        System.exit(0);
      }
      else if (ACTIVATE_CMD.equals(command)) {
        response = frontend.activate();
        System.out.println(response);
      }
      else if (DEACTIVATE_CMD.equals(command)) {
        response = frontend.deactivate();
        System.out.println(response);
      }
      else if (DUMP_CMD.equals(command)) {
        response = frontend.dump();
        System.out.println(response);
      } else {
        System.out.println("Command not found.");
      }

      System.out.printf("%n");
    }
  }
}
