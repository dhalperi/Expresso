package util;

import com.google.common.collect.Range;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class AutomatonUtil {
  public static String getShortestExample(Automaton a, boolean accepted) {
    String singleton = a.getSingleton();
    if (singleton != null) {
      if (accepted) return singleton;
      else if (singleton.length() > 0) return "";
      else return "\u0000";
    }
    return getShortestExample(a.getInitialState(), accepted);
  }

  private static String getShortestExample(State s, boolean accepted) {
    Map<State, String> path = new HashMap<>();
    LinkedList<State> queue = new LinkedList<>();
    path.put(s, "");
    queue.add(s);
    String best = null;
    while (!queue.isEmpty()) {
      State q = queue.removeFirst();
      String p = path.get(q);
      if (q.isAccept() == accepted) {
        if (best == null
            || p.length() < best.length()
            || (p.length() == best.length() && p.compareTo(best) < 0)) best = p;
      } else
        for (Transition t : q.getTransitions()) {
          State to = t.getDest();
          String tp = path.get(to);
          String np = p + randomChar(t.getMin(), t.getMax());
          if (tp == null || (tp.length() == np.length() && np.compareTo(tp) < 0)) {
            if (tp == null) queue.addLast(to);
            path.put(to, np);
          }
        }
    }
    return best;
  }

  // try to avoid garbled code
  private static char randomChar(char min, char max) {
    Range<Character> range1 = Range.closed(min, max);
    Range<Character> range2, intersect;

    // try to find integers first
    range2 = Range.closed('0', '9');
    if (range1.isConnected(range2) && !(intersect = range1.intersection(range2)).isEmpty()) {
      return intersect.lowerEndpoint();
    }

    range2 = Range.closed('a', 'z');
    if (range1.isConnected(range2) && !(intersect = range1.intersection(range2)).isEmpty()) {
      return intersect.lowerEndpoint();
    }

    range2 = Range.closed('A', 'Z');
    if (range1.isConnected(range2) && !(intersect = range1.intersection(range2)).isEmpty()) {
      return intersect.lowerEndpoint();
    }

    return (char) ((min + max) >> 1);
  }
}
