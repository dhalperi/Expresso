package controlplane.regex;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import org.junit.Test;
import atomic.automaton.AutomatonAtomicPredicates;
import datamodel.aspath.AsPathRegex;

import java.util.HashSet;
import java.util.Set;

public class AutomatonAtomicPredicatesTest {
  public static String AS_NUM_REGEX = "(0|[1-9][0-9]*)";
  public static String AS_PATH_LENGTH_0 = "";
  public static String AS_PATH_LENGTH_1 = AS_NUM_REGEX;
  public static String AS_PATH_LENGTH_2 = AS_NUM_REGEX + " " + AS_NUM_REGEX;
  public static String AS_PATH_LENGTH_3 = AS_NUM_REGEX + " " + AS_NUM_REGEX + " " + AS_NUM_REGEX;
  public static String AS_PATH_LENGTH_4 =
      AS_NUM_REGEX + " " + AS_NUM_REGEX + " " + AS_NUM_REGEX + " " + AS_NUM_REGEX;

  @Test
  public void testGetAtomicPredicateAutomata() {
    String fromCfg = "65000.*";

    Set<AsPathRegex> regexes = new HashSet<>();
    regexes.add(new AsPathRegex(AS_PATH_LENGTH_0));
    regexes.add(new AsPathRegex(AS_PATH_LENGTH_1));
    regexes.add(new AsPathRegex(AS_PATH_LENGTH_2));
    regexes.add(new AsPathRegex(AS_PATH_LENGTH_3));
    regexes.add(new AsPathRegex(AS_PATH_LENGTH_4));
    regexes.add(new AsPathRegex(fromCfg));

    String s = "^^65000 65001$";

    AutomatonAtomicPredicates<AsPathRegex> atomics =
        new AutomatonAtomicPredicates<>(regexes, AsPathRegex.ALL_AS_PATHS);
    atomics
        .getOriginToAtoms()
        .forEach(
            (regex, set) -> {
              System.out.println("================================");
              System.out.println(regex);
              set.forEach(
                  atomic -> {
                    Automaton automaton = atomics.getAtoms().get(atomic);
                    System.out.println("Match: " + automaton.run(s));
                  });
            });
  }

  @Test
  public void test() {
    RegExp r = new RegExp("65000.*");
    Automaton a = r.toAutomaton();
    String s = "65000";
    System.out.println("Match: " + a.run(s));
  }
}
