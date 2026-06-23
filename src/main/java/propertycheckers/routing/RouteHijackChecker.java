package propertycheckers.routing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.route.Route;
import datamodel.Range;
import datamodel.ipv4.PrefixRange;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import util.BddUtil;
import util.TimeUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static propertycheckers.JsonPrinter.printRoute;
import static propertycheckers.JsonPrinter.printRouteWithEnvc;
import static main.Controller.*;
import static util.MemorizeUtil.createDirIfAbsentElseClean;

public class RouteHijackChecker extends RoutingChecker {
  static int internalPrefixRange;

  // initialize it to avoid null pointer exception
  static HashMap<Router, HashMap<VirtualRouter, HashSet<Route>>> ALL_RESULTS = new HashMap<>();
  static HashMap<VirtualRouter, HashSet<Route>> ONE_RESULT;

  public static HashMap<Router, HashMap<VirtualRouter, HashSet<Route>>> check(boolean verbose) {
    loadInternalPrefixes();
    internalPrefixRange =
        BddUtil.orInBatch(
            Controller.bddManager.getBDD(),
            INTERNAL_PREFIXES.stream()
                .map(pfx -> PrefixRange.of(pfx, new Range<>(pfx.getPreLength(), 32)))
                .map(pr -> bddManager.getBddPrefixWrapper().encodePrefixRange(pr))
                .collect(Collectors.toSet()));

    ExpressoLogger.pushContext("HIJACK");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    ALL_RESULTS = new HashMap<>();
    cpExecutor.l3Topology.getNodes().values().forEach(RouteHijackChecker::check);

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();

    if (verbose) {
      OUTPUT = storage.output._outputBase.resolve(WITH_ENVC ? "hijack" : "hijack_without_envc");
      createDirIfAbsentElseClean(OUTPUT.toFile());
      if (DETAILS) printDetails();
      if (SUMMARY) printSummary();
    }

    return ALL_RESULTS;
  }

  public static void check(Router router) {
    ONE_RESULT = new HashMap<>();
    router.getVirtualRouters().values().forEach(RouteHijackChecker::check);
    if (!ONE_RESULT.isEmpty()) {
      ALL_RESULTS.put(router, ONE_RESULT);
    }
  }

  public static void check(VirtualRouter vrf) {
    if (!vrf.getVrfName().equals(Configuration.DEFAULT_VRF_NAME)) return;

    HashSet<Route> hijacked = new HashSet<>();
    for (Route route : vrf.getGlobalRib().getAllRoutes()) {
      if (isInternalRoute(route)) {
        int intersect = bddManager.and(route.getPrefixesBdd(), internalPrefixRange);
        if (intersect != 0) {
          HashMap<PrefixRange, Integer> map =
              bddManager.getBddPrefixWrapper().splitPrefixRangeEnvc(intersect);
          for (Map.Entry<PrefixRange, Integer> entry : map.entrySet()) {
            PrefixRange prefixRange = entry.getKey();
            int envc = entry.getValue();
            if (envc != 1) {
              hijacked.add(
                  route.toBuilder()
                      .setPrefixesBdd(bddManager.and(prefixRange.toBdd(), bddManager.not(envc)))
                      .build());
            }
          }
        }
      }
    }
    if (!hijacked.isEmpty()) {
      ONE_RESULT.put(vrf, hijacked);
    }
  }

  public static void printDetails() {
    ALL_RESULTS.keySet().forEach(RouteHijackChecker::print);
  }

  public static void print(Router router) {
    try {
      System.out.println(router.getRouterName());
      ONE_RESULT = ALL_RESULTS.get(router);
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve(router.getRouterName() + ".json").toFile()));
      RESULT_WRITER.setIndent(" ");
      RESULT_WRITER.beginObject();
      ONE_RESULT.keySet().forEach(RouteHijackChecker::print);
      RESULT_WRITER.endObject();
      RESULT_WRITER.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void print(VirtualRouter vrf) {
    try {
      RESULT_WRITER.name(vrf.getVrfName());
      RESULT_WRITER.beginArray();
      for (Route route : ONE_RESULT.get(vrf)) {
        printRouteWithEnvc(RESULT_WRITER, route);
      }
      RESULT_WRITER.endArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Multimap<PrefixRange, Route> summarize() {
    Multimap<PrefixRange, Route> summary = HashMultimap.create();
    ALL_RESULTS
        .values()
        .forEach(
            map ->
                map.forEach(
                    (vrf, routes) ->
                        routes.forEach(
                            route ->
                                route.getPrefixRanges().forEach(pr -> summary.put(pr, route)))));
    return summary;
  }

  public static void printSummary() {
    Multimap<PrefixRange, Route> summary = summarize();
    try {
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve("summary_pernode.json").toFile()));
      RESULT_WRITER.setIndent(" ");

      RESULT_WRITER.beginObject();
      for (Map.Entry<PrefixRange, Collection<Route>> e : summary.asMap().entrySet()) {
        RESULT_WRITER.name(e.getKey().toString());
        RESULT_WRITER.beginObject();
        int i = 0;
        for (Route route : e.getValue()) {
          RESULT_WRITER.name("" + i++);
          printRoute(RESULT_WRITER, route);
        }
        RESULT_WRITER.endObject();
      }
      RESULT_WRITER.endObject();

      RESULT_WRITER.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
