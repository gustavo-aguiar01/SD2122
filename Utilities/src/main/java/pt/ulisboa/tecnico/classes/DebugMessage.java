package pt.ulisboa.tecnico.classes;

public class DebugMessage {

    public static void debug(String debugMessage, boolean debugFlag) {
        if (debugFlag) {
            System.err.println("Debug : ");
            System.err.println("    - "  + debugMessage);
        }
    }
}
