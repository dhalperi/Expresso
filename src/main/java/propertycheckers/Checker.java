package propertycheckers;

import propertycheckers.forwarding.ForwardingChecker;
import propertycheckers.routing.RoutingChecker;
import application.info.ConfigInfo;
import com.google.gson.stream.JsonWriter;
import datamodel.ipv4.Prefix;
import main.Controller;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Checker {
  protected static Path OUTPUT;
  protected static boolean USE_JACKSON = true;
  protected static JsonWriter RESULT_WRITER;

  protected static Set<Prefix> INTERNAL_PREFIXES;

  protected static boolean DETAILS = true;
  protected static boolean SUMMARY = true;

  protected static void loadInternalPrefixes() {
    INTERNAL_PREFIXES = new HashSet<>();
    File file = Controller.storage.input._inputBase.resolve("internal-prefixes.txt").toFile();
    if (file.exists()) {
      try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        br.lines().forEach(line -> INTERNAL_PREFIXES.add(Prefix.of(line)));
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      INTERNAL_PREFIXES =
          ConfigInfo.collectInternalPrefixes().stream()
              .flatMap(pr -> pr.toPrefixes().stream())
              .collect(Collectors.toSet());
    }
  }

  public static void checkHijackConsistency() {
    RoutingChecker.RoutingCheckResult rResult = RoutingChecker.getResult();
    ForwardingChecker.ForwardingCheckResult fResult = ForwardingChecker.getResult();

    SortedSet<Prefix> rPrefixes =
        rResult.routeHijacks.values().stream()
            .flatMap(v -> v.values().stream())
            .flatMap(Collection::stream)
            .flatMap(route -> route.getPrefixRanges().stream())
            .flatMap(pr -> pr.toPrefixes().stream())
            .collect(Collectors.toCollection(TreeSet::new));

    SortedSet<Prefix> fPrefixes =
        fResult.hijacks.stream()
            .flatMap(node -> node.getPrefixes().stream())
            .collect(Collectors.toCollection(TreeSet::new));

    int rBdd =
        Controller.bddManager
            .getBddPrefixWrapper()
            .eraseLength(Controller.bddManager.getBddPrefixWrapper().encodePrefixes(rPrefixes));
    int fBdd =
        Controller.bddManager
            .getBddPrefixWrapper()
            .eraseLength(Controller.bddManager.getBddPrefixWrapper().encodePrefixes(fPrefixes));

    //    Set<Prefix> diff1 = SetUtil.difference(rPrefixes, fPrefixes, TreeSet::new);
    //    Set<Prefix> diff2 = SetUtil.difference(fPrefixes, rPrefixes, TreeSet::new);
    Set<Prefix> diff1 =
        Controller.bddManager
            .getBddPrefixWrapper()
            .extractPrefixesFromPacket(Controller.bddManager.minus1(rBdd, fBdd));
    Set<Prefix> diff2 =
        Controller.bddManager
            .getBddPrefixWrapper()
            .extractPrefixesFromPacket(Controller.bddManager.minus1(fBdd, rBdd));
    try (BufferedWriter bw =
        new BufferedWriter(
            new FileWriter(
                Controller.storage.output._outputBase.resolve("hijackDiff.json").toFile()))) {
      Map<String, Object> map = new TreeMap<>();
      map.put("routeHijackPrefixes", rPrefixes);
      map.put("trafficHijackPrefixes", fPrefixes);
      map.put("route-traffic", diff1);
      map.put("traffic-route", diff2);
      bw.write(JsonUtil.mapper.writeValueAsString(map));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
