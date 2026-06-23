package controlplane.process.isis;

import main.Configuration;
import main.ExpressoLogger;
import util.TimeUtil;

public class IsisExecutor {
  public static void execute(IsisTopology isisTopology) {
    TimeUtil isisTimer = new TimeUtil();
    isisTimer.begin();

    // first round
    isisTopology.getIsisProcesses().forEach(IsisRoutingProcess::begin);

    // iterate
    int iteration = iterate(isisTopology);
    ExpressoLogger.logCPExe(ExpressoLogger.LEVEL.INFO, "[ITERATION] " + iteration);

    isisTopology.getIsisProcesses().forEach(IsisRoutingProcess::mergeLevelRoutes);

    isisTimer.end(ExpressoLogger.LEVEL.INFO, "");
  }

  public static int iterate(IsisTopology isisTopology) {
    boolean converged = false;
    int iteration = 0;
    long start = System.nanoTime();
    while (!converged
        && iteration < Configuration.MAX_SRC_ROUNDS
        && (System.nanoTime() - start) / 1e9 < Configuration.SRC_TIMEOUT_SECONDS) {
      converged = true;

      // route exchanges
      isisTopology.getIsisProcesses().forEach(isis -> isis.routeOut(isisTopology));

      // isis local step
      for (IsisRoutingProcess isis : isisTopology.getIsisProcesses()) {
        converged = isis.updateRib() & converged;
      }

      iteration++;
    }
    return iteration;
  }
}
