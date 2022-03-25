package pt.ulisboa.tecnico.classes;

public class ErrorMessage {

    public static void fatalError (String errorMessage) {
        System.err.println("Error : ");
        System.err.println("    - "  + errorMessage);
        System.exit(-1);
    }

    public static void error (String errorMessage) {
        System.err.println("Error : ");
        System.err.println("    - "  + errorMessage);
    }
}
