package propertycheckers.routing;

import bdd.BddManager;
import controlplane.network.CPExecutor;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.route.BgpRoute;
import datamodel.Range;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import main.storage.Storage;
import util.BddUtil;
import util.JsonUtil;
import util.TimeUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static main.Controller.cpExecutor;
import static main.Controller.pushBDDManager;
import static main.Controller.pushCPExecutor;
import static main.Controller.pushStorage;
import static util.MemorizeUtil.createDirIfAbsentElseClean;

public class NoMartianChecker extends RoutingChecker {
  static final List<PrefixRange> MARTIANS =
      Stream.of(
              PrefixRange.of(Prefix.of("0.0.0.0/8"), new Range<>(8, 32)),
              PrefixRange.of(Prefix.of("10.0.0.0/8"), new Range<>(8, 32)),
              PrefixRange.of(Prefix.of("127.0.0.0/8"), new Range<>(8, 32)),
              PrefixRange.of(Prefix.of("169.254.0.0/16"), new Range<>(16, 32)),
              PrefixRange.of(Prefix.of("172.16.0.0/12"), new Range<>(12, 32)),
              PrefixRange.of(Prefix.of("192.0.2.0/24"), new Range<>(24, 32)),
              PrefixRange.of(Prefix.of("192.168.0.0/16"), new Range<>(16, 32)),
              PrefixRange.of(Prefix.of("198.18.0.0/15"), new Range<>(15, 32)),
              PrefixRange.of(Prefix.of("192.0.0.0/24"), new Range<>(24, 32)),
              PrefixRange.of(Prefix.of("128.0.0.0/16"), new Range<>(16, 32)),
              PrefixRange.of(Prefix.of("191.255.0.0/16"), new Range<>(16, 32)),
              PrefixRange.of(Prefix.of("223.255.255.0/24"), new Range<>(24, 32)))
          .collect(Collectors.toList());

  static SortedMap<Router, SortedMap<VirtualRouter, List<BgpRoute>>> violations =
      new TreeMap<>(Comparator.comparing(Router::getRouterName));

  public static SortedMap<Router, SortedMap<VirtualRouter, List<BgpRoute>>> check(
      boolean writeToJson) {
    loadInternalPrefixes();
    ExpressoLogger.pushContext("NO MARTIAN");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    int martian =
        BddUtil.orInBatch(
            Controller.bddManager.getBDD(),
            MARTIANS.stream()
                .map(pr -> Controller.bddManager.getBddPrefixWrapper().encodePrefixRange(pr))
                .collect(Collectors.toList()));

    violations = new TreeMap<>(Comparator.comparing(Router::getRouterName));
    for (Router router : Controller.cpExecutor.l3Topology.getNodes().values()) {
      for (VirtualRouter vrf : router.getVirtualRouters().values()) {
        if (vrf.getVrfName().equals(Configuration.DEFAULT_VRF_NAME)
            && vrf.getBgpProcess() != null) {
          List<BgpRoute> list =
              vrf.getBgpProcess().getRib().getAllRoutes().stream()
                  .map(
                      bgpRoute -> {
                        int i = Controller.bddManager.and(bgpRoute.getPrefixesBdd(), martian);
                        return i == 0 ? null : bgpRoute.toBuilder().setPrefixesBdd(i).build();
                      })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());
          violations
              .computeIfAbsent(
                  router, r -> new TreeMap<>(Comparator.comparing(VirtualRouter::getVrfName)))
              .put(vrf, list);
        }
      }
    }

    timer.end(ExpressoLogger.LEVEL.INFO, "");

    violations.forEach(
        (key, value) -> {
          int sum = value.values().stream().map(List::size).reduce(0, Integer::sum);
          ExpressoLogger.log(
              ExpressoLogger.LEVEL.INFO,
              String.format("router: %s, num violations: %d", key.getRouterName(), sum));
        });

    ExpressoLogger.popContext();

    if (writeToJson) {
      print();
    }

    return violations;
  }

  public static void print() {
    OUTPUT = Controller.storage.output._outputBase.resolve("no_martian");
    createDirIfAbsentElseClean(OUTPUT.toFile());
    try (BufferedWriter bw =
        new BufferedWriter(new FileWriter(OUTPUT.resolve("summary.json").toFile()))) {
      bw.write(JsonUtil.mapper.writeValueAsString(violations));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String... args) {
    Configuration.ENABLE_TRAFFIC_POLICY = false;
    Configuration.SYMBOLIC_COMMUNITY = false;
    Configuration.SYMBOLIC_COMMUNITY_AP = false;
    Configuration.SYMBOLIC_AS_PATH = false;

    Path net = Paths.get("networks/internet2");
    pushStorage(new Storage(net, null));

    pushBDDManager(new BddManager());
    pushCPExecutor(new CPExecutor());
    cpExecutor.init();
    cpExecutor.execute();

    check(true);
  }
}
