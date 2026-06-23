package propertycheckers.forwarding;

import application.info.ConfigInfo;
import bdd.BddManager;
import com.google.gson.stream.JsonWriter;
import controlplane.network.CPExecutor;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.route.Route;
import datamodel.ipv4.PrefixRange;
import main.Controller;
import main.ExpressoLogger;
import main.storage.Storage;
import util.BddUtil;
import util.TimeUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static propertycheckers.JsonPrinter.*;
import static util.MemorizeUtil.*;

@Deprecated
public class ReachabilityChecker {
  static Path OUTPUT;
  static Set<PrefixRange> INTERNAL_PREFIXES;
  static int INTERNAL_PREFIXES_BDD;

  public static void check(boolean verbose) {
    ExpressoLogger.pushContext("REACHABILITY");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    INTERNAL_PREFIXES = ConfigInfo.collectInternalPrefixes();
    INTERNAL_PREFIXES_BDD =
        BddUtil.orInBatch(
            Controller.bddManager.getBDD(),
            INTERNAL_PREFIXES.stream().map(PrefixRange::toBdd).collect(Collectors.toSet()));
    ALL_RESULTS = new HashMap<>();
    Controller.cpExecutor.l3Topology.getNodes().values().forEach(ReachabilityChecker::check);

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();

    if (verbose) {
      OUTPUT = Controller.storage.output._outputBase.resolve("reachability");
      createDirIfAbsent(OUTPUT.toFile());
      ALL_RESULTS.keySet().forEach(ReachabilityChecker::print);
    }
  }

  static HashMap<Router, HashMap<VirtualRouter, Integer>> ALL_RESULTS;
  static HashMap<VirtualRouter, Integer> ONE_RESULT;
  static JsonWriter RESULT_WRITER;

  public static void check(Router router) {
    ONE_RESULT = new HashMap<>();
    router.getVirtualRouters().values().forEach(ReachabilityChecker::check);
    ALL_RESULTS.put(router, ONE_RESULT);
  }

  public static void check(VirtualRouter vrf) {
    Set<Integer> predicates =
        vrf.getGlobalRib().getAllRoutes().stream()
            .map(Route::getPrefixesBdd)
            .collect(Collectors.toSet());
    int reachable = BddUtil.orInBatch(Controller.bddManager.getBDD(), predicates);
    reachable = Controller.bddManager.and(reachable, INTERNAL_PREFIXES_BDD);
    ONE_RESULT.put(vrf, reachable);
  }

  public static void print(Router router) {
    try {
      ONE_RESULT = ALL_RESULTS.get(router);
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve(router.getRouterName() + ".json").toFile()));
      RESULT_WRITER.setIndent(" ");
      RESULT_WRITER.beginObject();
      ONE_RESULT.keySet().forEach(ReachabilityChecker::print);
      RESULT_WRITER.endObject();
      RESULT_WRITER.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void print(VirtualRouter vrf) {
    try {
      RESULT_WRITER.name(vrf.getVrfName());
      RESULT_WRITER.beginObject();

      int reachable = ONE_RESULT.get(vrf);
      RESULT_WRITER.name("reachable");
      //            printPrefixRangeWithEnvc(RESULT_WRITER, reachable);
      printPrefixRange(RESULT_WRITER, reachable);

      //            int isolated = Controller.bddManager.not(reachable);
      int isolated = Controller.bddManager.minus(INTERNAL_PREFIXES_BDD, reachable);
      RESULT_WRITER.name("isolated");
      //            printPrefixRangeWithEnvc(RESULT_WRITER, isolated);
      printPrefixRange(RESULT_WRITER, isolated);

      RESULT_WRITER.endObject();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
