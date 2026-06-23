package propertycheckers.forwarding;

import com.google.common.collect.Multimap;
import datamodel.path.Path;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.Controller;
import main.ExpressoLogger;
import util.BddUtil;

import java.util.*;
import java.util.stream.Collectors;

public class TrafficHijackChecker extends ForwardingChecker {

  static boolean PER_NODE = false;

  public static Collection<ReachabilityNode> checkTrafficLeak(DPAnalyzer dpAnalyzer) {
    int internal = getInternalTraffic();
    Collection<ReachabilityNode> hijackedTraffic = new HashSet<>();
    for (Map.Entry<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> entry :
        dpAnalyzer.reachabilityDB.nodes.entrySet()) {
      if (checkOrigin(entry.getKey())) {
        Multimap<ReachabilityNode.Type, ReachabilityNode> reachTree = entry.getValue();
        Collection<ReachabilityNode> exitTraffic =
            reachTree.asMap().get(ReachabilityNode.Type.EXIT);
        if (exitTraffic == null) continue;
        if (PER_NODE) {
          int tmp =
              BddUtil.orInBatch(
                  Controller.bddManager.getBDD(),
                  exitTraffic.stream().map(ReachabilityNode::getPkt).collect(Collectors.toSet()));
          tmp = Controller.bddManager.and(tmp, internal);
          if (tmp != 0) {
            hijackedTraffic.add(ReachabilityNode.hijacked(new Path(entry.getKey()), tmp));
            ExpressoLogger.log(
                ExpressoLogger.LEVEL.INFO, entry.getKey().hopString() + " has hijacked traffic");
          }
        } else {
          Set<ReachabilityNode> leaked =
              exitTraffic.stream()
                  .map(
                      node -> {
                        int tmp = Controller.bddManager.and(internal, node.getPkt());
                        return tmp == 0 ? null : ReachabilityNode.hijacked(node.getPath(), tmp);
                      })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());
          if (!leaked.isEmpty()) {
            hijackedTraffic.addAll(leaked);
            ExpressoLogger.log(
                ExpressoLogger.LEVEL.INFO,
                String.format(
                    "%s has %d hijacked traffics", entry.getKey().hopString(), leaked.size()));
          }
        }
      }
    }
    return hijackedTraffic;
  }
}
