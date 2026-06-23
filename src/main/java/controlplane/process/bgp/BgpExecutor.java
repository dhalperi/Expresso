package controlplane.process.bgp;

import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.aspath.AsPathFactory;
import datamodel.community.CommunityListFactory;
import datamodel.ipv4.Prefix;
import datamodel.path.Path;
import datamodel.vendor.Juniper;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import util.TimeUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc4271">RFC-4271 BGPv4</a>
 *
 * <p>Bgp-Routers can be divided into three categories: Internal-Router (IR), Border-Router (BR) and
 * Route-Reflector (RR) Since Internal-Routers only receive announcements from internal peers and
 * have no route reflector clients. They are passive in the sense that they never send any
 * announcement. To determine the converged state of BGP, it suffices to simulate interactions
 * between BR and RR Once converged, we can run one BGP round to let IRs get routes.
 *
 * <p>Samuel Steffen, Timon Gehr, Petar Tsankov, Laurent Vanbever, and Martin Vechev. 2020.
 * Probabilistic Verification of Network Configurations.
 */
public class BgpExecutor {
  public static void execute(BgpTopology topology) {
    TimeUtil bgpTimer = new TimeUtil();
    bgpTimer.begin();

    // import routes from ISPs
    importFromIsp(topology);

    // bgp network
    // bgp default route advertise
    begin(topology);

    // iterate
    int iteration = iterate(topology);

    // final round: reflector to client
    finalRound(topology);

    // export routes to ISPs
    exportToIsp(topology);

    ExpressoLogger.logCPExe(ExpressoLogger.LEVEL.INFO, "[ITERATION] " + iteration);
    bgpTimer.end(ExpressoLogger.LEVEL.INFO, "");
  }

  public static void begin(BgpTopology topology) {
    // bgp network
    topology.edges.rowKeySet().forEach(BgpRoutingProcess::begin);

    // bgp default route advertise
    BgpRouteBuilder builder = new BgpRouteBuilder();
    builder.setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.ZERO));
    builder.setOrigin(BgpRoute.OriginType.INCOMPLETE); // keep consistent with Batfish
    topology
        .edges
        .rowMap()
        .forEach(
            (process, map) ->
                map.entrySet().stream()
                    .filter(
                        entry -> entry.getKey().ipv4UnicastAddressFamily.isDefaultRouteAdvertise())
                    .forEach(
                        entry -> {
                          BgpSessionProperties session = entry.getValue();
                          BgpRoutingProcess.propagate(
                              builder.setNextHopIp(session.local.localIp).build(), session);
                        }));
  }

  public static int iterate(BgpTopology topology) {
    boolean converged = false;
    int iteration = 0;
    long start = System.nanoTime();

    while (!converged
        && iteration < Configuration.MAX_SRC_ROUNDS
        && (System.nanoTime() - start) / 1e9 < Configuration.SRC_TIMEOUT_SECONDS) {
      converged =
          Configuration.BGP_SYNCHRONOUS
              ? synchronousIteration(topology)
              : asynchronousIteration(topology);
      iteration++;
    }

    return iteration;
  }

  private static boolean asynchronousIteration(BgpTopology topology) {
    //    TimeUtil.start();
    for (Map.Entry<BgpRoutingProcess, Map<BgpPeerConfig, BgpSessionProperties>> entry :
        topology.edges.rowMap().entrySet()) {
      BgpRoutingProcess process = entry.getKey();
      Map<BgpPeerConfig, BgpSessionProperties> peers = entry.getValue();
      process.updateRib();
      process.routeOut(peers, x -> !(x.local.process.isRR() && x.remote.process.isIR()));
    }
    //    TimeUtil.stop("BGP iteration");
    return topology.edges.rowKeySet().stream().allMatch(process -> process.inQueue.isEmpty());
  }

  private static boolean synchronousIteration(BgpTopology topology) {
    boolean converged = true;
    // exchange routes between bgp neighbors
    topology
        .edges
        .rowMap()
        .forEach(
            (process, peers) ->
                process.routeOut(peers, x -> !(x.local.process.isRR() && x.remote.process.isIR())));
    // bgp local step
    for (BgpRoutingProcess process : topology.edges.rowKeySet()) {
      converged = process.updateRib() & converged;
    }
    return converged;
  }

  public static void finalRound(BgpTopology topology) {
    topology.edges.rowKeySet().stream()
        .filter(BgpRoutingProcess::isRR)
        .forEach(p -> p.rrFinalRound(topology.edges.row(p)));
    topology.edges.rowKeySet().stream()
        .filter(BgpRoutingProcess::isIR)
        .forEach(BgpRoutingProcess::updateRib);
  }

  public static void importFromIsp(BgpTopology bgpTopology) {
    boolean juniper = Configuration.DEFAULT_VENDOR instanceof Juniper;
    bgpTopology
        .ispPeers
        .asMap()
        .forEach(
            (isp, configs) -> {
              // symbolic route
              BgpRoute bgpRoute =
                  new BgpRouteBuilder()
                      .setPrefixesBdd(Controller.bddManager.generateSymbolicRouteForISP(isp))
                      .setNextHopIp(isp.ip)
                      .setPath(new Path(isp))
                      .setAsPath(AsPathFactory.ofSingletonAsSets(isp.as))
                      .setCommunityList(
                          juniper ? CommunityListFactory.noBTE() : CommunityListFactory.arbitrary())
                      .setType(BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR)
                      .build();

              configs.forEach(config -> config.process.routeIn(bgpRoute, config, juniper));
            });
    bgpTopology.ispPeers.values().stream()
        .map(config -> config.process)
        .distinct()
        .forEach(BgpRoutingProcess::updateRib);
  }

  public static void exportToIsp(BgpTopology bgpTopology) {
    bgpTopology
        .ispPeers
        .asMap()
        .forEach(
            (isp, configs) -> {
              ArrayList<BgpRoute> routes = new ArrayList<>();
              configs.forEach(
                  config ->
                      routes.addAll(
                          config.process.multipathBgpRib.getAllRoutes().stream()
                              .flatMap(
                                  route ->
                                      BgpRoutingProcess.processOut(route.prepareOut(), config)
                                          .stream())
                              .collect(Collectors.toSet())));
              isp.rib.sortInsert(routes);
            });
  }
}
