package algorithm.bool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javafx.util.Pair;
import util.MathUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of Quine-McCluskey method for minimization of boolean expression.
 *
 * <p><a href="https://arxiv.org/ftp/arxiv/papers/1410/1410.1059.pdf"></a> <a
 * href="https://en.wikipedia.org/wiki/Quine%E2%80%93McCluskey_algorithm"></a>
 */
public abstract class QuineMcCluskey {
  int nVar;
  Map<Integer, Minterm> trueMinterms;
  Map<Integer, Minterm> dontCareMinterms;

  Map<Integer, Set<Minterm>> groups;
  // prime implicant
  Set<Minterm> PIs;
  // essential prime implicant
  Set<Minterm> EPIs;

  /**
   * Initialize with decimal minterms, we have to convert decimal minterms into their binary
   * representations (see {@link MathUtil#calBinRep(long, int)}).
   *
   * @param nVar number of variables
   * @param trueMinterms true minterms
   * @param dontCareMinterms don't care minterms
   */
  public QuineMcCluskey(int nVar, Set<Integer> trueMinterms, Set<Integer> dontCareMinterms) {
    this.nVar = nVar;
    this.trueMinterms = convert(trueMinterms);
    this.dontCareMinterms = convert(dontCareMinterms);
    this.groups = new HashMap<>();
    this.PIs = new HashSet<>();
    this.EPIs = new HashSet<>();
  }

  abstract Minterm getMinterm(int decimal);

  Map<Integer, Minterm> convert(Set<Integer> decimals) {
    Map<Integer, Minterm> map = new HashMap<>();
    for (int decimal : decimals) {
      map.put(decimal, getMinterm(decimal));
    }
    return map;
  }

  /**
   * Initialize with binary minterms, we have to convert binary minterms into their decimal
   * representations. The binary minterms can have wildcard bits. E.g., we will convert [0, -1] into
   * ([0, 0], 0) and ([0, 1], 2)
   */
  public QuineMcCluskey(int nVar, byte[][] trueMinterms, byte[][] dontCareMinterms) {
    this.nVar = nVar;
    this.trueMinterms = convert(trueMinterms);
    this.dontCareMinterms = convert(dontCareMinterms);
    this.groups = new HashMap<>();
    this.PIs = new HashSet<>();
    this.EPIs = new HashSet<>();
  }

  Map<Integer, Minterm> convert(byte[][] bvs) {
    long start = System.nanoTime();
    Map<Integer, Minterm> map = new HashMap<>();
    for (byte[] bv : bvs) {
      convert(bv, map);
    }
    long end = System.nanoTime();
    //        System.out.printf("convert time: %fs%n", (end - start) / 1e9);
    return map;
  }

  void convert(byte[] bv, Map<Integer, Minterm> map) {
    convert(bv, Arrays.copyOf(bv, bv.length), bv.length - 1, 0, 0, map);
  }

  abstract Minterm getMinterm(byte[] binary, int decimal, int nOne);

  void convert(byte[] bv, byte[] stk, int idx, int nOne, int minterm, Map<Integer, Minterm> map) {
    if (idx < 0) {
      map.put(minterm, getMinterm(Arrays.copyOf(stk, stk.length), minterm, nOne));
    } else {
      byte[] tmp = (bv[idx] != -1 ? new byte[] {bv[idx]} : new byte[] {0, 1});
      for (byte i : tmp) {
        stk[idx] = i;
        convert(bv, stk, idx - 1, nOne + i, (minterm << 1) + i, map);
        stk[idx] = bv[idx];
      }
    }
  }

  void group() {
    Stream.concat(trueMinterms.values().stream(), dontCareMinterms.values().stream())
        .forEach(
            minterm -> groups.computeIfAbsent(minterm.getNumberOfOne(), HashSet::new).add(minterm));
  }

  List<Map<Integer, Set<Minterm>>> columns = new LinkedList<>();

  boolean iterate() {
    long start = System.nanoTime();
    Map<Integer, Set<Minterm>> newGroups = new HashMap<>();
    boolean finish = true;
    for (int nOne : groups.keySet()) {
      if (groups.containsKey(nOne + 1)) {
        Set<Minterm> l1 = groups.get(nOne);
        Set<Minterm> l2 = groups.get(nOne + 1);
        for (Minterm n1 : l1) {
          boolean paired = false;
          for (Minterm n2 : l2) {
            Minterm n3 = n1.pair(n2);
            if (n3 != null) {
              paired = true;
              newGroups.computeIfAbsent(n3.nOne, HashSet::new).add(n3);
            }
          }
          if (paired) {
            finish = false;
          } else {
            PIs.add(n1);
          }
        }
      }
    }
    columns.add(groups);
    if (!finish) {
      groups = newGroups;
    }
    long end = System.nanoTime();
    //        System.out.printf("iterate time: %fs%n", (end - start) / 1e9);
    return finish;
  }

  int minSolutionSize = Integer.MAX_VALUE;

  void collect() {
    long start = System.nanoTime();
    groups.values().forEach(list -> PIs.addAll(list));

    Multimap<Integer, Minterm> counter = HashMultimap.create();
    for (Minterm PI : PIs) {
      for (int member : PI.members) {
        if (!dontCareMinterms.containsKey(member)) {
          counter.put(member, PI);
        }
      }
    }

    Stack<Multimap<Integer, Minterm>> counterStk = new Stack<>();
    counterStk.add(counter);
    Stack<HashSet<Minterm>> epiStk = new Stack<>();
    epiStk.add(new HashSet<>());
    minSolutionSize = Integer.MAX_VALUE;
    collectRec(counterStk, epiStk, 0);

    long end = System.nanoTime();
    //        System.out.printf("collect time: %fs%n", (end - start) / 1e9);
  }

  void collectRec(
      Stack<Multimap<Integer, Minterm>> counterStk,
      Stack<HashSet<Minterm>> epiStk,
      int curSolutionSize) {
    if (!counterStk.isEmpty() && curSolutionSize < minSolutionSize) {
      Multimap<Integer, Minterm> curCounter = counterStk.lastElement();
      if (curCounter.isEmpty()) {
        EPIs = epiStk.stream().flatMap(HashSet::stream).collect(Collectors.toSet());
        minSolutionSize = EPIs.size();
      } else {
        HashSet<Minterm> curWinners = new HashSet<>();
        curCounter.asMap().entrySet().stream()
            .filter(entry -> entry.getValue().size() == 1)
            .forEach(entry -> curWinners.addAll(entry.getValue()));
        if (!curWinners.isEmpty()) {
          enter(counterStk, epiStk, curCounter, curWinners);
          collectRec(counterStk, epiStk, curSolutionSize + curWinners.size());
          exit(counterStk, epiStk);
        } else {
          HashSet<Minterm> unusedPIs = new HashSet<>(PIs);
          epiStk.forEach(unusedPIs::removeAll);
          List<Minterm> sortedUnusedPIs =
              unusedPIs.stream()
                  .map(
                      u -> {
                        long n = u.getMembers().stream().filter(curCounter::containsKey).count();
                        return new Pair<>(n, u);
                      })
                  .filter(pair -> pair.getKey() > 0)
                  .sorted(Comparator.comparing(Pair::getKey, Comparator.reverseOrder()))
                  .map(Pair::getValue)
                  .collect(Collectors.toList());
          for (Minterm unusedPI : sortedUnusedPIs) {
            // this means we've found a new solution, can stop iterating the remaining unusedPIs
            if (curSolutionSize + 1 >= minSolutionSize) return;
            HashSet<Minterm> tmp = new HashSet<>();
            tmp.add(unusedPI);
            enter(counterStk, epiStk, curCounter, tmp);
            collectRec(counterStk, epiStk, curSolutionSize + 1);
            exit(counterStk, epiStk);
          }
        }
      }
    }
  }

  void enter(
      Stack<Multimap<Integer, Minterm>> counterStk,
      Stack<HashSet<Minterm>> epiStk,
      Multimap<Integer, Minterm> curCounter,
      HashSet<Minterm> curWinners) {
    Set<Integer> coveredMinterms =
        curWinners.stream()
            .flatMap(curRoundWinner -> curRoundWinner.members.stream())
            .collect(Collectors.toSet());

    Multimap<Integer, Minterm> nxtCounter = HashMultimap.create();
    curCounter
        .asMap()
        .forEach(
            (minterm, set) -> {
              if (!coveredMinterms.contains(minterm)) {
                nxtCounter.putAll(minterm, set);
              }
            });

    counterStk.add(nxtCounter);
    epiStk.add(curWinners);
  }

  void exit(Stack<Multimap<Integer, Minterm>> counterStk, Stack<HashSet<Minterm>> epiStk) {
    counterStk.pop();
    epiStk.pop();
  }

  public Set<Minterm> minimize() {
    long start = System.nanoTime();

    group();

    // grouping and pairing
    boolean finish = false;
    while (!finish) {
      finish = iterate();
    }

    collect();

    long end = System.nanoTime();
    System.out.println((end - start) / 1e9 + "s");

    return EPIs;
  }
}
