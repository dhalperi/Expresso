package propertycheckers.forwarding;

import com.google.common.collect.Multimap;
import controlplane.process.bgp.ISP;
import datamodel.path.Path;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.Controller;
import util.BddUtil;

import java.util.*;
import java.util.stream.Collectors;

public class OutboundReachability extends ForwardingChecker {

  public static Collection<ReachabilityNode> checkOutboundReachability(DPAnalyzer dpAnalyzer) {
    int internal = getInternalTraffic();
    Collection<ReachabilityNode> cannotReach = new HashSet<>();
    for (Map.Entry<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> entry :
        dpAnalyzer.reachabilityDB.nodes.entrySet()) {
      if (checkOrigin(entry.getKey())) {
        int outboundReach = 0;
        if (entry.getKey() instanceof ISP) continue;
        Multimap<ReachabilityNode.Type, ReachabilityNode> reachTree = entry.getValue();
        Collection<ReachabilityNode> exitTraffic =
            reachTree.asMap().get(ReachabilityNode.Type.EXIT);
        if (exitTraffic == null) continue;
        int tmp =
            BddUtil.orInBatch(
                Controller.bddManager.getBDD(),
                exitTraffic.stream().map(ReachabilityNode::getPkt).collect(Collectors.toList()));
        outboundReach = Controller.bddManager.or(outboundReach, tmp);
        outboundReach =
            Controller.bddManager.and(
                Controller.bddManager.not(internal), Controller.bddManager.not(outboundReach));
        cannotReach.add(ReachabilityNode.unreachable(new Path(entry.getKey()), outboundReach));
      }
    }
    return cannotReach;
  }
}
