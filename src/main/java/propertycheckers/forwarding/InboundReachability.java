package propertycheckers.forwarding;

import com.google.common.collect.Multimap;
import controlplane.process.bgp.ISP;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.Controller;
import util.BddUtil;

import java.util.*;
import java.util.stream.Collectors;

public class InboundReachability extends ForwardingChecker {

  public static Collection<ReachabilityNode> checkInboundReachability(DPAnalyzer dpAnalyzer) {
    int internal = getInternalTraffic();
    Collection<ReachabilityNode> cannotReach = new HashSet<>();
    for (Map.Entry<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> entry :
        dpAnalyzer.reachabilityDB.nodes.entrySet()) {
      if (checkOrigin(entry.getKey())) {
        int inboundReach = 0;
        if (entry.getKey() instanceof ISP) continue;
        Multimap<ReachabilityNode.Type, ReachabilityNode> reachTree = entry.getValue();
        Collection<ReachabilityNode> arriveTraffic = new HashSet<>();
        Collection<ReachabilityNode> tmp1 =
            reachTree.asMap().get(ReachabilityNode.Type.ARRIVE_LOOPBACK);
        Collection<ReachabilityNode> tmp2 =
            reachTree.asMap().get(ReachabilityNode.Type.ARRIVE_NULL0);
        arriveTraffic.addAll(tmp1 == null ? Collections.emptyList() : tmp1);
        arriveTraffic.addAll(tmp2 == null ? Collections.emptyList() : tmp2);
        int tmp =
            BddUtil.orInBatch(
                Controller.bddManager.getBDD(),
                arriveTraffic.stream().map(ReachabilityNode::getPkt).collect(Collectors.toList()));
        inboundReach = Controller.bddManager.or(inboundReach, tmp);
        inboundReach = Controller.bddManager.and(internal, Controller.bddManager.not(inboundReach));
        cannotReach.add(
            ReachabilityNode.unreachable(new datamodel.path.Path(entry.getKey()), inboundReach));
      }
    }
    return cannotReach;
  }

}
