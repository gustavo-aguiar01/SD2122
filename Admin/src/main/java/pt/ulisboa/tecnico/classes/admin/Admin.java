package pt.ulisboa.tecnico.classes.admin;

import java.util.Scanner;
import java.util.Arrays;

import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;

public class Admin {

  private static final String EXIT_CMD = "exit";
  private static final String ACTIVATE_CMD = "activate";
  private static final String DEACTIVATE_CMD = "deactivate";
  private static final String DUMP_CMD = "dump";

  public static void main(String[] args) {


    System.out.println(Admin.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    final String host = "localhost";
    final int port = 8080;

    AdminFrontend frontend = new AdminFrontend(host, port);

    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.printf("> ");
      String[] line = scanner.nextLine().split(" ");
      String command = line[0];
      String[] arguments = Arrays.copyOfRange(line, 1, line.length);

      if (EXIT_CMD.equals(command)) {
        scanner.close();
        return;

      } else if (ACTIVATE_CMD.equals(command)) {

        String response = frontend.activate();
        System.out.println(" - " + response);

      } else if (DEACTIVATE_CMD.equals(command)) {

        String response = frontend.deactivate();
        System.out.println(" - " + response);

      } else if (DUMP_CMD.equals(command)) {

        String response = frontend.dump();
        System.out.println(" - " + response);

      }
    }
  }
}
