package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class Student {

  private static final String HOST = "localhost";
  private static final int port = 8080;

  private static final String ENROLL_CMD = "inscrever";
  private static final String LIST_CMD = "listar";

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
        studentFrontend.enroll(id, name);
      }

      if (LIST_CMD.equals(line)) {
        System.out.print(studentFrontend.listClass());
      }
    }
  }
}
