package propertycheckers;

import algorithm.bool.Minterm;
import bdd.BddPrefixWrapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import controlplane.rib.Rib;
import controlplane.rib.RibRouteSet;
import controlplane.route.Route;
import datamodel.BitVector;
import datamodel.ipv4.PrefixRange;
import javafx.util.Pair;
import main.Controller;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static propertycheckers.EnvcMinimizer.minimizedEnvcs;

public class JsonPrinter {
  public static void printPrefixRangeWithEnvc(JsonWriter jw, int predicate) throws IOException {
    jw.beginArray();
    for (Map.Entry<PrefixRange, Collection<String>> entry :
        getPrefixRangesWithEnvc(predicate).entrySet()) {
      jw.beginObject();

      // print prefix range
      jw.name("prefixRange");
      jw.value(entry.getKey().toString());

      // print environment condition
      jw.name("envc");
      printEnvc(jw, entry.getValue());

      jw.endObject();
    }
    jw.endArray();
  }

  public static void printEnvc(JsonWriter jw, Collection<String> envcs) throws IOException {
    List<String[]> list =
        envcs.stream()
            .map(envc -> envc.split(" & "))
            .sorted(Comparator.comparingInt(envc -> envc.length))
            .collect(Collectors.toList());
    jw.beginArray();
    for (String[] envc : list) {
      if (envc.length == 1 && envc[0].equals("")) {
        jw.value("true");
      } else {
        jw.beginArray();
        for (String part : envc) jw.value(part);
        jw.endArray();
      }
    }
    jw.endArray();
  }

  public static void printPrefixRange(JsonWriter jw, int predicate) throws IOException {
    jw.beginArray();
    for (PrefixRange pr :
        Controller.bddManager.getBddPrefixWrapper().extractPrefixRanges(predicate)) {
      jw.value(pr.toString());
    }
    jw.endArray();
  }

  public static void printRouteWithEnvc(JsonWriter jw, Route route) throws IOException {
    printRoute(jw, route, true);
  }

  public static void printRoute(JsonWriter jw, Route route) throws IOException {
    printRoute(jw, route, false);
  }

  public static void printRibWithEnvc(JsonWriter jw, Rib<? extends Route> rib) throws IOException {
    printRib(jw, rib, true);
  }

  public static void printRib(JsonWriter jw, Rib<? extends Route> rib) throws IOException {
    printRib(jw, rib, false);
  }

  static void printRoute(JsonWriter jw, Route route, boolean envc) throws IOException {
    jw.beginObject();

    if (envc) {
      // print prefix and envc
      jw.name("prefixRangesWithEnvc");
      printPrefixRangeWithEnvc(jw, route.getPrefixesBdd());
    } else {
      // print prefix
      jw.name("prefixRanges");
      printPrefixRange(jw, route.getPrefixesBdd());
    }

    // print attributes
    jw.name("attributes");
    jw.value(route.getAttributes().toString());

    jw.endObject();
  }

  static void printRib(JsonWriter jw, Rib<? extends Route> rib, boolean envc) throws IOException {
    jw.beginArray();
    for (RibRouteSet<?> ribRouteSet : rib.getRib().values()) {
      jw.beginArray();
      for (Route route : ribRouteSet.getAllRoutes()) {
        printRoute(jw, route, envc);
      }
      jw.endArray();
    }
    jw.endArray();
  }

  public static Map<PrefixRange, Collection<String>> getPrefixRangesWithEnvc(int predicate) {
    return getPrefixRangesWithEnvc(predicate, true);
  }

  public static Map<PrefixRange, Collection<String>> getPrefixRangesWithEnvc(
      int predicate, boolean minimize) {
    int n1 = Controller.bddManager.getBddEnvcWrapper().numIspVar();
    int n2 = BddPrefixWrapper.prefixBits;

    Multimap<BitVector, BitVector> bvMap = HashMultimap.create();
    HashSet<byte[]> bvs = Controller.bddManager.getBitVectors(predicate);
    bvs.forEach(
        bv -> {
          byte[] prBv = Arrays.copyOfRange(bv, n1, n1 + n2);
          byte[] envcBv = Arrays.copyOfRange(bv, 0, n1);
          bvMap.put(new BitVector(prBv), new BitVector(envcBv));
        });

    Multimap<PrefixRange, String> prefixRangesWithEnvc = HashMultimap.create();
    bvMap
        .asMap()
        .forEach(
            (prBv, envcBvs) -> {
              Set<PrefixRange> prSet = getPrefixRanges(prBv);
              Set<String> envcSet;
              // envcSet =
              //     (minimize && envcBvs.size() < 20)
              //         ? getMinimizedEnvcs(envcBvs)
              //         : getEnvcs(envcBvs);
              // envcSet = minimize ? getMinimizedEnvcs(envcBvs) : getEnvcs(envcBvs);
              // envcSet = minimize ? getMinimizedEnvcs(envcBvs, 10) : getEnvcs(envcBvs);
              envcSet = getEnvcs(envcBvs);
              prSet.forEach(pr -> prefixRangesWithEnvc.putAll(pr, envcSet));
            });
    return prefixRangesWithEnvc.asMap();
  }

  public static Set<PrefixRange> getPrefixRanges(BitVector prBv) {
    return PrefixRange.bin2PrefixRanges(prBv.getBv());
  }

  public static Set<String> getEnvcs(Collection<BitVector> envcBvs) {
    return envcBvs.stream()
        .map(envcBv -> Controller.bddManager.getBddEnvcWrapper().getEnvc(envcBv.getBv()))
        .collect(Collectors.toSet());
  }

  public static Set<String> getMinimizedEnvcs(Collection<BitVector> envcBvs) {
    Pair<List<Integer>, Set<Minterm>> pair = minimizedEnvcs(envcBvs, envcBvs.size());
    return EnvcMinimizer.toString(pair.getKey(), pair.getValue());
  }

  public static Set<String> getMinimizedEnvcs(Collection<BitVector> envcBvs, int groupSize) {
    Pair<List<Integer>, Set<Minterm>> pair = minimizedEnvcs(envcBvs, groupSize);
    return EnvcMinimizer.toString(pair.getKey(), pair.getValue());
  }
}
