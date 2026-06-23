package propertycheckers.routing;

import com.google.gson.stream.JsonWriter;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.rib.Rib;
import controlplane.route.Route;
import datamodel.ipv4.Prefix;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import util.TimeUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static propertycheckers.JsonPrinter.printRib;
import static propertycheckers.JsonPrinter.printRibWithEnvc;
import static util.MemorizeUtil.createDirIfAbsentElseClean;

@Deprecated
public class TrafficLeakChecker extends RoutingChecker {

  public static void check(boolean verbose) {
    loadInternalPrefixes();

    ExpressoLogger.pushContext("TRAFFIC LEAK");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    ALL_RESULTS = new HashMap<>();
    Controller.cpExecutor.l3Topology.getNodes().values().forEach(TrafficLeakChecker::check);

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();

    if (verbose) {
      OUTPUT =
          Controller.storage.output._outputBase.resolve(
              WITH_ENVC ? "traffic_leak" : "traffic_leak_without_envc");
      createDirIfAbsentElseClean(OUTPUT.toFile());
      if (DETAILS) printDetails();
      if (SUMMARY) printSummary();
    }
  }

  static HashMap<Router, HashMap<VirtualRouter, HashMap<Prefix, Rib<Route>>>> ALL_RESULTS;
  static HashMap<VirtualRouter, HashMap<Prefix, Rib<Route>>> ONE_RESULT;

  public static void check(Router router) {
    ONE_RESULT = new HashMap<>();
    router.getVirtualRouters().values().forEach(TrafficLeakChecker::check);
    if (!ONE_RESULT.isEmpty()) {
      ALL_RESULTS.put(router, ONE_RESULT);
    }
  }

  public static void check(VirtualRouter vrf) {
    if (!vrf.getRouter().getRouterName().contains("pr")
        || !vrf.getVrfName().equals(Configuration.DEFAULT_VRF_NAME)) return;
    HashMap<Prefix, Rib<Route>> tmp = new HashMap<>();
    for (Prefix internalPrefix : INTERNAL_PREFIXES) {
      int internalPrefixBdd =
          Controller.bddManager.getBddPrefixWrapper().encodePrefix(internalPrefix);
      Rib<Route> rib = new Rib<>();
      for (Route route : vrf.getGlobalRib().getAllRoutes()) {
        int intersect = Controller.bddManager.and(internalPrefixBdd, route.getPrefixesBdd());
        if (intersect != 0) {
          Route newRoute = Route.clone(route);
          newRoute.setPrefixesBdd(intersect);
          rib.simpleInsert(newRoute);
        }
      }
      if (!rib.getRib().isEmpty()) {
        boolean allExternal =
            rib.getAllRoutes().stream().noneMatch(TrafficLeakChecker::isInternalRoute);
        if (allExternal) {
          tmp.put(internalPrefix, rib);
        }
      }
    }
    if (!tmp.isEmpty()) {
      ONE_RESULT.put(vrf, tmp);
    }
  }

  public static void printDetails() {
    ALL_RESULTS.keySet().forEach(TrafficLeakChecker::print);
  }

  public static void print(Router router) {
    try {
      ONE_RESULT = ALL_RESULTS.get(router);
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve(router.getRouterName() + ".json").toFile()));
      RESULT_WRITER.setIndent(" ");
      RESULT_WRITER.beginObject();
      ONE_RESULT.keySet().forEach(TrafficLeakChecker::print);
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
      for (Map.Entry<Prefix, Rib<Route>> entry : ONE_RESULT.get(vrf).entrySet()) {
        Prefix internalPrefix = entry.getKey();
        Rib<Route> rib = entry.getValue();

        RESULT_WRITER.beginObject();

        RESULT_WRITER.name("internalPrefix");
        RESULT_WRITER.value(internalPrefix.toString());

        RESULT_WRITER.name("routes");
        if (WITH_ENVC) {
          printRibWithEnvc(RESULT_WRITER, rib);
        } else {
          printRib(RESULT_WRITER, rib);
        }

        RESULT_WRITER.endObject();
      }
      RESULT_WRITER.endArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printSummary() {
    Set<Prefix> summary =
        ALL_RESULTS.values().stream()
            .flatMap(map1 -> map1.values().stream().flatMap(map2 -> map2.keySet().stream()))
            .collect(Collectors.toSet());
    summary.retainAll(INTERNAL_PREFIXES);
    try {
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve("summary_pernode.json").toFile()));
      RESULT_WRITER.setIndent(" ");

      RESULT_WRITER.beginObject();
      RESULT_WRITER.name("prefixes");
      RESULT_WRITER.beginArray();
      for (Prefix prefix : summary) {
        RESULT_WRITER.value(prefix.toString());
      }
      RESULT_WRITER.endArray();
      RESULT_WRITER.endObject();

      RESULT_WRITER.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
