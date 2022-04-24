package pt.ulisboa.tecnico.classes;

import java.util.Map;
import java.util.stream.Collectors;

public class DebugMessage {

    public static void debug(String debugMessage, String function, boolean debugFlag) {
        if (debugFlag) {
            if (function != null)
                System.err.println("Debug (" + function + ") : ");

            System.err.println(("    - "  + debugMessage).replace("\n", "\n      "));
        }
    }

    public static String timestampToString(Map<String, Integer> timestamp) {
        return timestamp.keySet().stream().map(q ->  q + " : " + timestamp.get(q) + "\n")
                .collect(Collectors.joining());
    }
}
