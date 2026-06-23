package controlplane.network;

import datamodel.Owner;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Ipv4Owners;
import datamodel.ipv4.Prefix;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Since jgrapht's min cut algorithm is not tested on multigraph, we cannot guarantee the simulation
 * of "links always up" will get the correct answer. Fortunately, it is correct for our test cases.
 */
public class L3Topology {
  TreeMap<String, Router> _nodes;
  TreeMap<InterfaceName, Interface> _interfaces;
  HashSet<L3Edge> _l3Edges;

  Owner<Prefix, VirtualRouter> _prefixOwners;
  Owner<Ip, VirtualRouter> _ipOwners;
  Ipv4Owners _ipv4Owners;

  public L3Topology(
      TreeMap<String, Router> nodes,
      TreeMap<InterfaceName, Interface> interfaces,
      HashSet<L3Edge> l3Edges) {
    _nodes = nodes;
    _interfaces = interfaces;
    _l3Edges = l3Edges;
    L3TopologyHelper.buildIpPrefixOwner(this);
  }

  public TreeMap<String, Router> getNodes() {
    return _nodes;
  }

  public TreeMap<InterfaceName, Interface> getInterfaces() {
    return _interfaces;
  }

  public HashSet<L3Edge> getL3Edges() {
    return _l3Edges;
  }

  public Optional<L3Edge> getL3Edge(Interface head) {
    // no ip address configured, this is a layer 2 interface
    if (head.getAllIpAddresses().isEmpty()) {
      if (head.channelGroup != null) {
        return getL3Edge(head.router.getInterface(head.channelGroup));
      } else {
        return Optional.empty();
      }
    } else {
      return _l3Edges.stream()
          .filter(e -> e._port1.equals(head.interfaceName) || e._port2.equals(head.interfaceName))
          .findFirst();
    }
  }

  public Optional<Interface> getTheOtherEnd(Interface oneEnd) {
    if (oneEnd.getAllIpAddresses().isEmpty()) {
      if (oneEnd.channelGroup != null) {
        return getTheOtherEnd(oneEnd.router.getInterface(oneEnd.channelGroup));
      } else {
        return Optional.empty();
      }
    } else {
      return _l3Edges.stream()
          .map(e -> e.getTheOtherEnd(oneEnd.interfaceName))
          .filter(Objects::nonNull)
          .map(name -> _interfaces.get(name))
          .findFirst();
    }
  }

  public Owner<Prefix, VirtualRouter> getPrefixOwners() {
    return _prefixOwners;
  }

  public Owner<Ip, VirtualRouter> getIpOwners() {
    return _ipOwners;
  }

  public Ipv4Owners getIpv4Owners() {
    return _ipv4Owners;
  }
}
