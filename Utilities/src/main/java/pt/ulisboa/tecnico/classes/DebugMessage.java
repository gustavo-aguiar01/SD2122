package pt.ulisboa.tecnico.classes;

public class DebugMessage {

    public static void debug(String debugMessage, String function, boolean debugFlag) {
        if (debugFlag) {
            if (function != null)
                System.err.println("Debug (" + function + ") : ");

            System.err.println(("    - "  + debugMessage).replace("\n", "\n      "));
        }
    }
}
