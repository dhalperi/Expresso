package propertycheckers.routing;

import controlplane.process.bgp.ISP;
import controlplane.route.BgpRoute;
import datamodel.path.BTE;
import main.Controller;
import main.ExpressoLogger;
import util.JsonUtil;
import util.TimeUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static main.Controller.cpExecutor;
import static util.MemorizeUtil.createDirIfAbsentElseClean;

/**
 * This is a customized checker for finding BlockToExternal violations on the internet2
 * configuration. Specifically, the BlockToExternal property requires that routes with the BTE
 * community should never be exported to any external-neighbors.
 */
public class BlockToExternalChecker extends RoutingChecker {
  static SortedMap<ISP, List<BgpRoute>> violations =
      new TreeMap<>(Comparator.comparing(ISP::getName));
  static SortedMap<String, SortedMap<String, Object>> summaries;

  public static SortedMap<ISP, List<BgpRoute>> check(boolean writeToJson) {
    loadInternalPrefixes();
    ExpressoLogger.pushContext("BLOCK TO EXTERNAL");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    violations = find();
    summaries = summarize();

    timer.end(ExpressoLogger.LEVEL.INFO, "");

    summaries.forEach(
        (key, value) ->
            ExpressoLogger.log(
                ExpressoLogger.LEVEL.INFO,
                String.format("Router: %s, num violations: %s", key, value.get("number"))));

    ExpressoLogger.popContext();

    if (writeToJson) {
      print();
    }

    return violations;
  }

  public static SortedMap<ISP, List<BgpRoute>> find() {
    SortedMap<ISP, List<BgpRoute>> tmp = new TreeMap<>(Comparator.comparing(ISP::getName));
    for (ISP isp : cpExecutor.bgpTopology.getISPs()) {
      List<BgpRoute> list =
          isp.getRib().getAllRoutes().stream()
              .filter(bgpRoute -> bgpRoute.getPath().contains(BTE.INSTANCE))
              .collect(Collectors.toList());
      if (!list.isEmpty()) {
        tmp.put(isp, list);
      }
    }
    return tmp;
  }

  public static SortedMap<String, SortedMap<String, Object>> summarize() {
    SortedMap<String, List<String>> tmp = new TreeMap<>();
    violations
        .keySet()
        .forEach(
            isp ->
                cpExecutor
                    .bgpTopology
                    .getISPPeers()
                    .get(isp)
                    .forEach(
                        cfg -> {
                          String router = cfg.getProcess().getVrf().getRouter().getRouterName();
                          tmp.computeIfAbsent(router, r -> new LinkedList<>()).add(isp.getName());
                        }));
    return tmp.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                  SortedMap<String, Object> map = new TreeMap<>();
                  map.put("details", e.getValue());
                  map.put("number", e.getValue().size());
                  return map;
                },
                (v1, v2) -> v1,
                TreeMap::new));
  }

  public static void print() {
    print(DETAILS, SUMMARY);
  }

  public static void print(boolean details, boolean summary) {
    OUTPUT = Controller.storage.output._outputBase.resolve("block_to_external");
    createDirIfAbsentElseClean(OUTPUT.toFile());
    try (BufferedWriter bw =
        new BufferedWriter(new FileWriter(OUTPUT.resolve("summary.json").toFile()))) {

      SortedMap<String, Object> map = new TreeMap<>();
      if (summary) map.put("summary", summaries);
      if (details) map.put("violations", violations);

      bw.write(JsonUtil.mapper.writeValueAsString(map));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
