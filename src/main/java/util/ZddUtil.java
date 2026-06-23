package util;

import jdd.zdd.ZDD;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ZddUtil {
  /**
   * Each integer in {@code batch} is a set of variable sets, e.g., {{v1}, {v1, v2}}. <br>
   * This method computes the union of all sets in {@code batch}.
   */
  public static int unionInBatch(ZDD zdd, Collection<Integer> batch) {
    int ret = zdd.empty();
    for (int i : batch) {
      int tmp = zdd.ref(zdd.union(ret, i));
      zdd.deref(ret);
      ret = tmp;
    }
    return ret;
  }

  /**
   * Each integer in {@code batch} is a set of variable sets, e.g., {{v1}, {v1, v2}}. <br>
   * This method computes the intersection of all sets in {@code batch}.
   */
  public static int intersectInBatch(ZDD zdd, Collection<Integer> batch) {
    int ret = zdd.universe();
    for (int i : batch) {
      int tmp = zdd.ref(zdd.intersect(ret, i));
      zdd.deref(ret);
      ret = tmp;
    }
    return ret;
  }

  /**
   * Build a set of sets containing single variables. <br>
   * E.g., if {@code vars} equals {v1, v2, v3}, then returns {{v1}, {v2}, {v3}}
   */
  public static int allSingletonSets(ZDD zdd, int[] vars) {
    return allSingletonSets(zdd, Arrays.stream(vars).boxed().collect(Collectors.toSet()));
  }

  /**
   * Build a set of sets containing single variables. <br>
   * E.g., if {@code vars} equals {v1, v2, v3}, then returns {{v1}, {v2}, {v3}}
   */
  public static int allSingletonSets(ZDD zdd, Collection<Integer> vars) {
    Iterator<Integer> itr = vars.iterator();
    int ret = zdd.single(itr.next());
    while (itr.hasNext()) {
      int tmp = zdd.ref(zdd.union(ret, zdd.single(itr.next())));
      zdd.deref(ret);
      ret = tmp;
    }
    return ret;
  }

  public static HashSet<boolean[]> allSat(ZDD zdd, int numVars, int predicate) {
    boolean[] bin = new boolean[numVars];
    Arrays.fill(bin, false);
    HashSet<boolean[]> collector = new HashSet<>();
    allSatRec(zdd, predicate, bin, collector);
    return collector;
  }

  private static void allSatRec(
      ZDD zdd, int predicate, boolean[] bin, HashSet<boolean[]> collector) {
    if (predicate != 0) {
      if (predicate == 1) {
        collector.add(bin.clone());
      } else {
        int var = zdd.getVar(predicate);
        bin[var] = true;
        allSatRec(zdd, zdd.getHigh(predicate), bin, collector);
        bin[var] = false;
        allSatRec(zdd, zdd.getLow(predicate), bin, collector);
      }
    }
  }

  public static String allSatString(ZDD zdd, int numVars, int predicate) {
    HashSet<boolean[]> allSats = allSat(zdd, numVars, predicate);
    return "{"
        + allSats.stream()
            .map(
                oneSat ->
                    "{"
                        + IntStream.range(0, oneSat.length)
                            .mapToObj(i -> oneSat[i] ? "v" + i : null)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "))
                        + "}")
            .sorted()
            .collect(Collectors.joining(", "))
        + "}";
  }
}
