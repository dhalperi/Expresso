package propertycheckers.forwarding;

import com.google.common.collect.Multimap;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.ExpressoLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class BlackholeChecker extends ForwardingChecker {

  public static Collection<ReachabilityNode> checkBlackhole(DPAnalyzer dpAnalyzer) {
    Collection<ReachabilityNode> anomalies = new HashSet<>();
    for (Map.Entry<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> entry :
        dpAnalyzer.reachabilityDB.nodes.entrySet()) {
      if (checkOrigin(entry.getKey())) {
        Collection<ReachabilityNode> blackholes =
            entry.getValue().asMap().get(ReachabilityNode.Type.BLACKHOLE);
        if (blackholes == null) continue;
        anomalies.addAll(blackholes);
        ExpressoLogger.log(
            ExpressoLogger.LEVEL.INFO,
            String.format("%s has %d blackholes", entry.getKey().hopString(), blackholes.size()));
      }
    }
    return anomalies;
  }

}
