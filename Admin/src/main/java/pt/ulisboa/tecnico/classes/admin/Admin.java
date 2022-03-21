package pt.ulisboa.tecnico.classes.admin;

import java.util.Scanner;
import pt.ulisboa.tecnico.classes.admin.AdminFrontend;

public class Admin {

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
      String line = scanner.nextLine();

      System.out.println(line);
    }
  }
}
