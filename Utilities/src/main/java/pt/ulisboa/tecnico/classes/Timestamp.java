package pt.ulisboa.tecnico.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class Timestamp {
    public Map<String, Integer> timestamp = new HashMap<>();

    public Timestamp () {
    }

    public Timestamp (Map<String, Integer> timestamp) {
        this.timestamp.putAll(timestamp);
    }

    public Map<String, Integer> getMap() { return timestamp; }

    public void put(String sa, int count) {
        this.timestamp.put(sa, count);
    }

    public int get(String sa) {
        return this.timestamp.get(sa);
    }

    public boolean contains(String sa) {
        return this.timestamp.containsKey(sa);
    }

    public void merge(Timestamp other) {
        other.getMap().keySet().forEach(sa ->
        { if (!timestamp.containsKey(sa)) {
            timestamp.put(sa, other.get(sa)); } else {
            timestamp.put(sa, max(timestamp.get(sa), other.getMap().get(sa))); }
        });
    }

    public boolean biggerThan(Timestamp other) {
        Map<String, Integer> o = other.getMap();
        return o.keySet().stream().allMatch(sa -> (!timestamp.containsKey(sa) && o.get(sa) == 0) ||
                (timestamp.containsKey(sa) && timestamp.get(sa) >= o.get(sa)));
    }

    @Override
    public String toString() {
        return "(" + String.join(", ", timestamp.keySet().stream().map(q ->  q + " : " + timestamp.get(q))
                .collect(Collectors.toList())) + ")";
    }
}
