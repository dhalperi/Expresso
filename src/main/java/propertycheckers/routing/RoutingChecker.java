package propertycheckers.routing;

import propertycheckers.Checker;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.ISP;
import controlplane.route.BgpRoute;
import controlplane.route.DynamicRoute;
import controlplane.route.Route;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.path.Path;
import datamodel.path.PathHop;
import main.Controller;
import util.JsonUtil;
import util.MemorizeUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static application.info.AsInfo.isInternalPrivateAS;

public class RoutingChecker extends Checker {
  static boolean WITH_ENVC = false;

  public static boolean ROUTE_LEAK = true;
  public static boolean ROUTE_HIJACK = true;

  private static HashMap<ISP, List<BgpRoute>> routeLeaks;
  private static HashMap<Router, HashMap<VirtualRouter, HashSet<Route>>> routeHijacks;
  public static int numLeakedPrefixes;
  public static int numHijackedPrefixes;

  public static boolean isInternalRoute(Route route) {
    if (route instanceof DynamicRoute<?, ?>) {
      Optional<PathHop> origin = ((DynamicRoute<?, ?>) route).getPath().getOrigin();
      if (origin.isPresent() && (origin.get() instanceof ISP)) {
        long as = ((ISP) origin.get()).getAs();
        return isInternalPrivateAS(as);
      }
    }
    return true;
  }

  public static void check() {
    check(ROUTE_LEAK, ROUTE_HIJACK);
  }

  public static void check(boolean leak, boolean hijack) {
    routeLeaks = leak ? GaoRexfordChecker.check(false) : new HashMap<>();
    routeHijacks = hijack ? RouteHijackChecker.check(false) : new HashMap<>();
  }

  public static void print() {
    print(DETAILS, SUMMARY);
  }

  public static void print(boolean details, boolean summary) {
    OUTPUT =
        Controller.storage.output._outputBase.resolve(
            "cp_anomalies_" + Controller.cpExecutor.bgpTopology.getISPs().size());
    MemorizeUtil.createDirIfAbsentElseClean(OUTPUT.toFile());
    if (details) printDetails();
    if (summary) printSummary();
  }

  private static void printDetails() {
    // print route leaks
    for (Map.Entry<ISP, List<BgpRoute>> e : routeLeaks.entrySet()) {
      try (BufferedWriter bw =
          new BufferedWriter(
              new FileWriter(OUTPUT.resolve(e.getKey().getName() + ".json").toFile()))) {
        bw.write(
            JsonUtil.mapper.writeValueAsString(ImmutableSortedMap.of("routeLeaks", e.getValue())));
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    // print route hijacks
    for (Map.Entry<Router, HashMap<VirtualRouter, HashSet<Route>>> e : routeHijacks.entrySet()) {
      try (BufferedWriter bw =
          new BufferedWriter(
              new FileWriter(OUTPUT.resolve(e.getKey().getRouterName() + ".json").toFile()))) {
        bw.write(
            JsonUtil.mapper.writeValueAsString(
                ImmutableSortedMap.of("routeHijacks", e.getValue())));
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private static void printSummary() {
    SortedMap<String, Object> summary = new TreeMap<>();
    // print route leak details
    Multimap<PrefixRange, Path> leaksSummary = GaoRexfordChecker.summarize();
    //    List<Prefix> leakedPrefixes = getPrefixes(leaksSummary.keySet());
    //    numLeakedPrefixes = leakedPrefixes.size();
    Map<Prefix, Object> leakedPrefixes = getPrefixesWithOwner(leaksSummary.keySet());
    numLeakedPrefixes = leakedPrefixes.keySet().size();
    System.out.println(
        leakedPrefixes.keySet().stream()
            .sorted()
            .map(Prefix::toString)
            .collect(Collectors.joining("\n")));
    summary.put("numLeakedPrefixes", numLeakedPrefixes);
    summary.put("leakedPrefixes", leakedPrefixes);
    summary.put("routeLeaks", leaksSummary.asMap());

    System.out.println("--------------------------------");

    // print route hijack details
    Multimap<PrefixRange, Route> hijackSummary = RouteHijackChecker.summarize();
    //    List<Prefix> hijackedPrefixes = getPrefixes(hijackSummary.keySet());
    //    numHijackedPrefixes = hijackedPrefixes.size();
    Map<Prefix, Object> hijackedPrefixes = getPrefixesWithOwner(hijackSummary.keySet());
    numHijackedPrefixes = hijackedPrefixes.keySet().size();
    System.out.println(
        hijackedPrefixes.keySet().stream()
            .sorted()
            .map(Prefix::toString)
            .collect(Collectors.joining("\n")));
    summary.put("numHijackedPrefixes", numHijackedPrefixes);
    summary.put("hijackedPrefixes", hijackedPrefixes);
    summary.put("routeHijacks", hijackSummary.asMap());

    try (BufferedWriter bw =
        new BufferedWriter(new FileWriter(OUTPUT.resolve("summary.json").toFile()))) {
      bw.write(JsonUtil.mapper.writeValueAsString(summary));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<Prefix> getPrefixes(Collection<PrefixRange> prefixRanges) {
    return prefixRanges.stream()
        .flatMap(pr -> pr.toPrefixes().stream())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  private static Map<Prefix, Object> getPrefixesWithOwner(Collection<PrefixRange> prefixRanges) {
    return prefixRanges.stream()
        .flatMap(pr -> pr.toPrefixes().stream())
        .distinct()
        .collect(
            Collectors.toMap(
                pfx -> pfx,
                pfx -> Controller.cpExecutor.l3Topology.getIpv4Owners().findExactOwners(pfx),
                (v1, v2) -> v1,
                TreeMap::new));
  }

  public static class RoutingCheckResult {
    public final HashMap<ISP, List<BgpRoute>> routeLeaks;
    public final HashMap<Router, HashMap<VirtualRouter, HashSet<Route>>> routeHijacks;

    public RoutingCheckResult(
        HashMap<ISP, List<BgpRoute>> routeLeaks,
        HashMap<Router, HashMap<VirtualRouter, HashSet<Route>>> routeHijacks) {
      this.routeLeaks = routeLeaks;
      this.routeHijacks = routeHijacks;
    }
  }

  public static RoutingCheckResult getResult() {
    return new RoutingCheckResult(routeLeaks, routeHijacks);
  }
}
