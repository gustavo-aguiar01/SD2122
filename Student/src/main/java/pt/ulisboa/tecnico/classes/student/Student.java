package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.ErrorMessage;

import java.util.Scanner;

public class Student {

  private static final String HOST = "localhost";
  private static final int port = 8080;

  private static final String ENROLL_CMD = "enroll";
  private static final String LIST_CMD = "list";
  private static final String EXIT_CMD = "exit";

  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);
    if (args.length < 2) {
      ErrorMessage.fatalError("Invalid command expected : alunoXXXX <nome>*, where XXXX is 4 digit positive number" );
    }
    final String id = args[0];

    StringBuilder nameBuilder = new StringBuilder(args[1]);
    for (int i = 2; i < args.length; i++) {
      nameBuilder.append(" " + args[i]);
    }
    final String name = nameBuilder.toString();

    final ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, port).usePlaintext().build();
    final StudentFrontend studentFrontend = new StudentFrontend(channel);

    while (true) {
      System.out.printf("> ");
      String line = scanner.nextLine();

      if (ENROLL_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.enroll(id, name));
        } catch (StatusRuntimeException e) {
          ErrorMessage.error(e.getStatus().getDescription());
        }
      }

      if (LIST_CMD.equals(line)) {
        try {
          System.out.println(studentFrontend.listClass());
        } catch (StatusRuntimeException e) {
          ErrorMessage.error(e.getStatus().getDescription());
        }
      }

      if (EXIT_CMD.equals(line)) {
        break;
      }

      System.out.printf("%n");
    }
    channel.shutdown();
  }
}
