package dataplane.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import datamodel.path.Path;
import datamodel.path.PathHop;
import javafx.util.Pair;
import main.Configuration;

import javax.annotation.Nullable;
import java.util.*;

import static main.Controller.bddManager;
import static main.Controller.cpExecutor;

public class ReachabilityDB {
  public HashMap<PathHop, Multimap<ReachabilityNode.Type, ReachabilityNode>> nodes;
  HashMap<VirtualRouter, HashMap<Interface, Integer>> intfPredicates;
  HashMap<VirtualRouter, Integer> vrfPredicates;

  AclVisitor aclVisitor;

  public ReachabilityDB(
      HashMap<VirtualRouter, HashMap<Interface, Integer>> intfPredicates,
      HashMap<VirtualRouter, Integer> vrfPredicates) {
    this.nodes = new HashMap<>();
    this.intfPredicates = intfPredicates;
    this.vrfPredicates = vrfPredicates;
    this.aclVisitor = new AclVisitor(bddManager.getBDD());
  }

  void build() {
    for (Router router : cpExecutor.l3Topology.getNodes().values()) {
      // todo: add filter here to start traversing only from nodes that are actually generating traffic
      for (VirtualRouter vrf : router.getVirtualRouters().values()) {
        if (!intfPredicates.containsKey(vrf)) continue;
        traverse(vrf, vrf, new Path(), 1, null);
      }
    }
  }

  void traverse(
      PathHop srcHop,
      VirtualRouter curVrf,
      Path curPath,
      int curPkt,
      @Nullable String appointedNexthopInterface) {
    // loop detected
    if (curPath.contains(curVrf)) {
      ReachabilityNode loopNode =
          new ReachabilityNode(new Path(curPath, curVrf), curPkt, ReachabilityNode.Type.LOOP);
      addNode(srcHop, loopNode);
      return;
    }

    // packets that cannot match any forwarding rules are dropped
    int blackholePkt = bddManager.minus1(curPkt, vrfPredicates.get(curVrf));
    if (blackholePkt != 0) {
      ReachabilityNode blackholeNode =
          new ReachabilityNode(
              new Path(curPath, curVrf), blackholePkt, ReachabilityNode.Type.BLACKHOLE);
      addNode(srcHop, blackholeNode);
    }

    // find nexthops
    Map<Interface, Integer> nexthops;
    if (appointedNexthopInterface != null) {
      Interface nexthop = curVrf.getRouter().getInterface(appointedNexthopInterface);
      nexthops = Collections.singletonMap(nexthop, intfPredicates.get(curVrf).get(nexthop));
    } else {
      nexthops = intfPredicates.get(curVrf);
    }

    // packets that can match one or more forwarding rules are forwarded
    for (Map.Entry<Interface, Integer> entry : nexthops.entrySet()) {
      Interface outIntf = entry.getKey();
      int intfPredicate = entry.getValue();
      int outPkt = bddManager.and1(curPkt, intfPredicate);

      // apply outbound ACL
      if (outPkt != 0 && Configuration.ENABLE_ACL) {
        outPkt = bddManager.and1(outPkt, aclVisitor.visitAcl(outIntf.getOutboundAcl()).getKey());
      }
      // apply outbound traffic policy
      if (outPkt != 0
          && Configuration.ENABLE_TRAFFIC_POLICY
          && outIntf.getOutboundTrafficPolicy() != null) {
        outPkt =
            new TrafficPolicyVisitor(outPkt)
                .visitOutboundTrafficPolicy(outIntf.getOutboundTrafficPolicy());
      }

      if (outPkt != 0) {
        if (outIntf.getType() == Interface.InterfaceType.LOOPBACK) {
          ReachabilityNode loopbackNode =
              new ReachabilityNode(
                  new Path(curPath, curVrf, outIntf),
                  outPkt,
                  ReachabilityNode.Type.ARRIVE_LOOPBACK);
          addNode(srcHop, loopbackNode);
        } else if (outIntf.getType() == Interface.InterfaceType.NULL) {
          ReachabilityNode nullNode =
              new ReachabilityNode(
                  new Path(curPath, curVrf, outIntf), outPkt, ReachabilityNode.Type.ARRIVE_NULL0);
          addNode(srcHop, nullNode);
        } else {
          Optional<Interface> optional = cpExecutor.l3Topology.getTheOtherEnd(outIntf);
          if (optional.isPresent()) {
            Interface inIntf = optional.get();

            // apply inbound ACL
            if (Configuration.ENABLE_ACL) {
              outPkt =
                  bddManager.and1(outPkt, aclVisitor.visitAcl(inIntf.getInboundAcl()).getKey());
            }
            // apply inbound traffic policy
            if (Configuration.ENABLE_TRAFFIC_POLICY && inIntf.getInboundTrafficPolicy() != null) {
              List<Pair<Integer, String>> list =
                  new TrafficPolicyVisitor(outPkt)
                      .visitInboundTrafficPolicy(inIntf.getInboundTrafficPolicy());
              list.forEach(
                  p ->
                      traverse(
                          srcHop,
                          inIntf.getVrf(),
                          new Path(curPath, curVrf, outIntf, inIntf),
                          p.getKey(),
                          p.getValue()));
            } else if (outPkt != 0) {
              traverse(
                  srcHop,
                  inIntf.getVrf(),
                  new Path(curPath, curVrf, outIntf, inIntf),
                  outPkt,
                  null);
            }
          } else if (cpExecutor.bgpTopology.getEdgeInterfaces().contains(outIntf)) {
            ReachabilityNode exitNode =
                new ReachabilityNode(
                    new Path(curPath, curVrf, outIntf), outPkt, ReachabilityNode.Type.EXIT);
            addNode(srcHop, exitNode);
          } else {
            ReachabilityNode arriveNode =
                new ReachabilityNode(
                    new Path(curPath, curVrf, outIntf), outPkt, ReachabilityNode.Type.ARRIVE);
            addNode(srcHop, arriveNode);
          }
        }
      }
    }
  }

  private void addNode(PathHop srcHop, ReachabilityNode node) {
    nodes.computeIfAbsent(srcHop, hop -> HashMultimap.create()).put(node.type, node);
  }
}
