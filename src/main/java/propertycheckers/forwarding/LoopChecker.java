package propertycheckers.forwarding;

import com.google.common.collect.Multimap;
import controlplane.network.VirtualRouter;
import datamodel.ipv4.Prefix;
import datamodel.path.Path;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.ExpressoLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class LoopChecker extends ForwardingChecker {
  static boolean filterUninterestingLoops = true;

  private static boolean interestingLoopPath(Path path) {
    // only care about the smallest loops
    if (path.get(0).equals(path.get(path.size() - 1))) {
      if (!filterUninterestingLoops) return true;
      // ignore loops containing only two vrfs
      return path.getHops().stream()
              .filter(hop -> hop instanceof VirtualRouter)
              .collect(Collectors.toSet())
              .size()
          > 2;
    }
    return false;
  }

  public static Collection<ReachabilityNode> checkLoop(DPAnalyzer dpAnalyzer) {
    Collection<ReachabilityNode> anomalies = new HashSet<>();
    for (Map.Entry<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> entry :
        dpAnalyzer.reachabilityDB.nodes.entrySet()) {
      if (checkOrigin(entry.getKey())) {
        Collection<ReachabilityNode> loops =
            entry.getValue().asMap().get(ReachabilityNode.Type.LOOP);
        if (loops == null) continue;

        loops =
            loops.stream()
                // only care about the smallest loops
                .filter(loop -> interestingLoopPath(loop.getPath()))
                .map(
                    loop -> {
                      SortedSet<Prefix> prefixes =
                          loop.getPrefixes().stream()
                              .filter(prefix -> prefix.getPreLength() < 31)
                              .collect(Collectors.toCollection(TreeSet::new));
                      if (prefixes.isEmpty()) {
                        return null;
                      } else {
                        return ReachabilityNode.loop(loop.getPath(), prefixes);
                      }
                    })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!loops.isEmpty()) {
          anomalies.addAll(loops);
          ExpressoLogger.log(
              ExpressoLogger.LEVEL.INFO,
              String.format("%s has %d loops", entry.getKey().hopString(), loops.size()));
        }
      }
    }
    return anomalies;
  }
}
