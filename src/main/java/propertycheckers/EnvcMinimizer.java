package propertycheckers;

import algorithm.bool.DecimalQMC;
import algorithm.bool.Minterm;
import algorithm.bool.QuineMcCluskey;
import datamodel.BitVector;
import javafx.util.Pair;
import main.Controller;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class is used to minimize an ENVC (external route environment condition, referred to as
 * advertiser condition in the paper) boolean formula. It is really slow, use it only on small
 * networks.
 */
public class EnvcMinimizer {
  public static Pair<List<Integer>, Set<Minterm>> minimizedEnvcs(
      Collection<BitVector> envcBvs, int groupSize) {
    byte[][] tmp0 = new byte[envcBvs.size()][];
    int i = 0;
    for (BitVector bv : envcBvs) {
      tmp0[i++] = bv.getBv();
    }

    List<Integer> tmp1 = IntStream.range(0, tmp0[0].length).boxed().collect(Collectors.toList());

    return minimizedEnvcs(tmp0, tmp1, groupSize);
  }

  public static Pair<List<Integer>, Set<Minterm>> minimizedEnvcs(
      byte[][] envcBvs, List<Integer> curBits, int groupSize) {
    if (envcBvs.length <= groupSize) {
      List<Integer> usedBits = getUsedBits(envcBvs, curBits);

      byte[][] envcMinterms = new byte[envcBvs.length][];
      int i = 0;
      for (byte[] envcBv : envcBvs) {
        envcMinterms[i++] = getMinterm(usedBits, curBits, envcBv);
      }

      QuineMcCluskey alg = new DecimalQMC(usedBits.size(), envcMinterms, new byte[0][]);
      Set<Minterm> envcEPIs = alg.minimize();

      return new Pair<>(usedBits, envcEPIs);
    } else {
      // divide groups
      List<byte[][]> groups = getGroups(envcBvs, groupSize);

      // minimize each group
      List<Pair<List<Integer>, Set<Minterm>>> groupResults =
          groups.stream()
              .map(group -> minimizedEnvcs(group, curBits, groupSize))
              .collect(Collectors.toList());

      // collect all used bits
      List<Integer> allUsedBits =
          groupResults.stream()
              .flatMap(pair -> pair.getKey().stream())
              .distinct()
              .sorted()
              .collect(Collectors.toList());

      int n = groupResults.stream().map(pair -> pair.getValue().size()).reduce(0, Integer::sum);
      byte[][] minterms = new byte[n][];
      int i = 0;
      for (Pair<List<Integer>, Set<Minterm>> pair : groupResults) {
        for (Minterm minterm : pair.getValue()) {
          minterms[i++] = getMinterm(allUsedBits, pair.getKey(), minterm.getBinary());
        }
      }

      Optional<Integer> m =
          groupResults.stream().map(pair -> pair.getValue().size()).max(Integer::compareTo);

      return minimizedEnvcs(
          minterms, allUsedBits, m.orElse((Integer.max(n, groupSize) + groupSize) >> 1));
    }
  }

  static List<Integer> getUsedBits(byte[][] envcBvs, List<Integer> bits) {
    HashSet<Integer> hs = new HashSet<>();
    for (byte[] envcBv : envcBvs) {
      for (int i = 0; i < envcBv.length; i++) {
        if (envcBv[i] != -1) {
          hs.add(bits.get(i));
        }
      }
    }
    return hs.stream().sorted().collect(Collectors.toList());
  }

  static List<byte[][]> getGroups(byte[][] envcBvs, int groupSize) {
    List<byte[][]> groups = new LinkedList<>();
    int i = 0;
    while (i < envcBvs.length) {
      int s = Integer.min(groupSize, envcBvs.length - i);
      if (s > 0) {
        byte[][] group = new byte[s][];
        int j = 0;
        while (j < s) {
          group[j++] = envcBvs[i++];
        }
        groups.add(group);
      }
    }
    return groups;
  }

  public static byte[] getMinterm(List<Integer> dstBits, List<Integer> srcBits, byte[] src) {
    byte[] dst = new byte[dstBits.size()];
    int p1 = 0;
    int p2 = 0;
    while (p2 < dstBits.size()) {
      while (p1 < srcBits.size() && srcBits.get(p1) < dstBits.get(p2)) p1++;
      dst[p2] =
          (p1 < srcBits.size() && Objects.equals(srcBits.get(p1), dstBits.get(p2))) ? src[p1] : -1;
      p2++;
    }
    return dst;
  }

  public static Set<String> toString(List<Integer> usedBits, Set<Minterm> minterms) {
    List<String> ispNames =
        usedBits.stream()
            .map(bit -> Controller.bddManager.getBddEnvcWrapper().getIsp(bit).getName())
            .collect(Collectors.toList());
    return minterms.stream()
        .map(envcEPI -> envcEPI.boolString(ispNames))
        .collect(Collectors.toSet());
  }
}
