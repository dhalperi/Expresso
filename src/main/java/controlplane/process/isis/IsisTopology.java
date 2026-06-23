package controlplane.process.isis;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import controlplane.network.*;
import org.batfish.datamodel.isis.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.batfish.datamodel.isis.IsisLevel.*;

public class IsisTopology {
  L3Topology l3Topology;
  Network<IsisNode, IsisEdge> intfEdges;

  public IsisTopology() {}

  public void build(L3Topology l3Topo) {
    l3Topology = l3Topo;
    Set<IsisEdge> edgeSet =
        l3Topo.getL3Edges().stream()
            .map(l3Edge -> edgeIfCircuit(l3Topo, l3Edge))
            .flatMap(List::stream)
            .collect(ImmutableSet.toImmutableSet());
    MutableNetwork<IsisNode, IsisEdge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    ImmutableSet.Builder<IsisNode> nodeSet = ImmutableSet.builder();
    edgeSet.forEach(
        edge -> {
          nodeSet.add(edge.getNode1());
          nodeSet.add(edge.getNode2());
        });
    nodeSet.build().forEach(graph::addNode);
    edgeSet.forEach(edge -> graph.addEdge(edge.getNode1(), edge.getNode2(), edge));
    intfEdges = ImmutableNetwork.copyOf(graph);
  }

  private static List<IsisEdge> edgeIfCircuit(L3Topology l3Topology, L3Edge l3Edge) {
    Interface if1 = l3Topology.getInterfaces().get(l3Edge.getSrc());
    Interface if2 = l3Topology.getInterfaces().get(l3Edge.getDst());
    IsisRoutingProcess isis1 = if1.getVrf().getIsisProcess();
    IsisRoutingProcess isis2 = if2.getVrf().getIsisProcess();
    if (isis1 == null
        || isis2 == null
        || if1.getIsisIntfSettings() == null
        || if2.getIsisIntfSettings() == null) {
      return Collections.emptyList();
    }

    // make sure system IDs are distinct
    if (Arrays.equals(
        isis1.process.getNetAddress().getSystemId(), isis2.process.getNetAddress().getSystemId())) {
      return Collections.emptyList();
    }

    boolean sameArea =
        Arrays.equals(
            isis1.process.getNetAddress().getAreaId(), isis2.process.getNetAddress().getAreaId());

    IsisLevel circuitType =
        IsisLevel.intersection(
            processLevel(isis1.process),
            interfaceSettingsLevel(if1.getIsisIntfSettings()),
            processLevel(isis2.process),
            interfaceSettingsLevel(if2.getIsisIntfSettings()),
            sameArea ? LEVEL_1_2 : LEVEL_2);
    if (circuitType == null) {
      return Collections.emptyList();
    }

    List<IsisEdge> list = new LinkedList<>();
    if (l3Edge.forward() || l3Edge.dual()) {
      list.add(
          new IsisEdge(
              circuitType,
              new IsisNode(
                  if1.getInterfaceName().getRouterName(),
                  if1.getInterfaceName().getInterfaceName()),
              new IsisNode(
                  if2.getInterfaceName().getRouterName(),
                  if2.getInterfaceName().getInterfaceName())));
    }
    if (l3Edge.backward() || l3Edge.dual()) {
      list.add(
          new IsisEdge(
              circuitType,
              new IsisNode(
                  if2.getInterfaceName().getRouterName(),
                  if2.getInterfaceName().getInterfaceName()),
              new IsisNode(
                  if1.getInterfaceName().getRouterName(),
                  if1.getInterfaceName().getInterfaceName())));
    }
    return list;
  }

  @Nullable
  private static IsisLevel processLevel(IsisProcess isisProcess) {
    return union(
        isisProcess.getLevel1() != null ? LEVEL_1 : null,
        isisProcess.getLevel2() != null ? LEVEL_2 : null);
  }

  @Nullable
  private static IsisLevel interfaceSettingsLevel(IsisInterfaceSettings settings) {
    return union(
        settings.getLevel1() != null ? LEVEL_1 : null,
        settings.getLevel2() != null ? LEVEL_2 : null);
  }

  public List<IsisRoutingProcess> getIsisProcesses() {
    return l3Topology.getNodes().values().stream()
        .flatMap(r -> r.getVirtualRouters().values().stream())
        .map(VirtualRouter::getIsisProcess)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public List<IsisEdge> inEdges(VirtualRouter vrf) {
    return intfEdges.edges().stream()
        .filter(
            edge ->
                vrf.getInterfaces()
                    .containsKey(
                        new InterfaceName(
                            edge.getNode2().getNode(), edge.getNode2().getInterfaceName())))
        .collect(Collectors.toList());
  }

  public Interface getInterface(IsisNode node) {
    return l3Topology
        .getInterfaces()
        .get(new InterfaceName(node.getNode(), node.getInterfaceName()));
  }

  public Network<IsisNode, IsisEdge> getNetwork() {
    return intfEdges;
  }
}
