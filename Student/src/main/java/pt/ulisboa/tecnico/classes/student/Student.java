package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Scanner;

public class Student {

  private static final String HOST = "localhost";
  private static final int port = 8080;

  private static final String ENROLL_CMD = "enroll";
  private static final String LIST_CMD = "list";
  private static final String EXIT_CMD = "exit";

  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);

    System.out.println(Student.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }

    final String id = args[0];
    final String name = args[1];

    final ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, port).usePlaintext().build();
    final StudentFrontend studentFrontend = new StudentFrontend(channel);

    while (true) {
      System.out.printf("> ");
      String line = scanner.nextLine();

      if (ENROLL_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.enroll(id, name));
        } catch (StatusRuntimeException e) {
          System.out.println("ERROR: " +
                  e.getStatus().getDescription());
        }
      }

      if (LIST_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.listClass());
        } catch (StatusRuntimeException e) {
          System.out.println("ERROR: " +
                  e.getStatus().getDescription());
        }
      }

      if (EXIT_CMD.equals(line)) {
        break;
      }

    }
    channel.shutdown();
  }
}
