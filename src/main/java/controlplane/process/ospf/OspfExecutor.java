package controlplane.process.ospf;

import controlplane.route.OspfRoute;
import controlplane.route.builder.OspfRouteBuilder;
import main.Configuration;
import main.ExpressoLogger;
import util.TimeUtil;

public class OspfExecutor {
  public static void execute(OspfTopology topology) {
    TimeUtil ospfTimer = new TimeUtil();
    ospfTimer.begin();

    // first round
    topology.edges.rowKeySet().forEach(OspfRoutingProcess::begin);

    // iterate
    int iteration = iterate(topology);
    ExpressoLogger.logCPExe(ExpressoLogger.LEVEL.INFO, "[ITERATION] " + iteration);

    ospfTimer.end(ExpressoLogger.LEVEL.INFO, "");
  }

  public static int iterate(OspfTopology topology) {
    boolean converged = false;
    int iteration = 0;
    long start = System.nanoTime();
    while (!converged
        && iteration < Configuration.MAX_SRC_ROUNDS
        && (System.nanoTime() - start) / 1e9 < Configuration.SRC_TIMEOUT_SECONDS) {
      converged = true;

      // route exchange between ospf neighbors
      topology.edges.rowMap().forEach(OspfRoutingProcess::routeOut);

      // todo propagate both intra-area routes and inter-area routes
      // ospf local step
      for (OspfRoutingProcess process : topology.edges.rowKeySet()) {
        converged = process.updateRib() & converged;
      }

      iteration++;
    }
    return iteration;
  }

  public static void propagate(OspfRoute route, OspfEdge edge) {
    OspfRouteBuilder builder = route.toBuilder();

    // extend propagation path
    builder.setPath(builder.getPath().append(edge.src.process.virtualRouter));

    // update next hop ip
    builder.setNextHopIp(edge.src.intf.getIp());

    // update next hop interface
    builder.setNextHopInterface(edge.dst.intf);

    // update metric
    builder.setMetric(route.getMetric() + edge.dst.intfSetting.ospfCost);

    edge.dst.process.routeIn(edge.dst.intf.getOspfIntfSetting().ospfArea, builder.build());
  }
}
