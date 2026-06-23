package controlplane.network;

import datamodel.Owner;
import datamodel.filterlist.FilterType;
import datamodel.ipv4.IpAddress;
import datamodel.ipv4.Ipv4Owners;
import main.Controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class L3TopologyHelper {
  private static SortedSet<IpAddress> _internalPrefixes;

  public static void buildIpPrefixOwner(L3Topology l3Topology) {
    loadInternalPrefixes();

    l3Topology._ipOwners = new Owner<>();
    l3Topology._prefixOwners = new Owner<>();
    l3Topology._ipv4Owners = new Ipv4Owners();

    // interface addresses
    l3Topology
        ._interfaces
        .values()
        .forEach(
            intf -> {
              if (intf.active) {
                VirtualRouter vrf = intf.vrf;
                addIpAddressToOwner(l3Topology, intf.primaryIpAddress, vrf, intf);
                intf.secondaryIpAddresses.forEach(
                    secIpAddr -> addIpAddressToOwner(l3Topology, secIpAddr, vrf, intf));
              }
            });

    // ospf network
    l3Topology._nodes.values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .flatMap(vr -> vr.getOspfProcesses().values().stream())
        .forEach(ospf -> {});

    // bgp network
    l3Topology._nodes.values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .map(VirtualRouter::getBgpProcess)
        .filter(Objects::nonNull)
        .forEach(
            bgp ->
                bgp.getNetworks().values().stream()
                    .flatMap(HashSet::stream)
                    .forEach(
                        prefix -> l3Topology._ipv4Owners.add(prefix, bgp.getVrf(), "bgpNetwork")));

    // ip-prefix filters
    l3Topology._nodes.values().stream()
        .filter(n -> n.getFilterLists().rowMap().get(FilterType.PREFIX) != null)
        .forEach(
            n ->
                n.getFilterLists()
                    .rowMap()
                    .get(FilterType.PREFIX)
                    .values()
                    .forEach(
                        fl ->
                            fl.collectPrefixRanges()
                                .forEach(
                                    prefixRange ->
                                        l3Topology._ipv4Owners.add(prefixRange, n, fl))));

    // static routes
    l3Topology._nodes.values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .forEach(
            vr -> {
              vr.getConditionalStaticRib()
                  .getAllRoutes()
                  .forEach(
                      sr ->
                          sr.getPrefixRanges().stream()
                              .flatMap(pr -> pr.toPrefixes().stream())
                              .forEach(pfx -> l3Topology._ipv4Owners.add(pfx, vr, "static route")));
              vr.getUnconditionalStaticRib()
                  .getAllRoutes()
                  .forEach(
                      sr ->
                          sr.getPrefixRanges().stream()
                              .flatMap(pr -> pr.toPrefixes().stream())
                              .forEach(pfx -> l3Topology._ipv4Owners.add(pfx, vr, "static route")));
            });

    _internalPrefixes.forEach(addr -> l3Topology._ipv4Owners.addInternalPrefix(addr));
  }

  private static void loadInternalPrefixes() {
    _internalPrefixes = new TreeSet<>();
    File file = Controller.storage.input._inputBase.resolve("internal-prefixes.txt").toFile();
    if (file.exists()) {
      try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        br.lines().forEach(line -> _internalPrefixes.add(IpAddress.parse(line)));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void addIpAddressToOwner(
      L3Topology l3Topology, IpAddress ipAddress, VirtualRouter vrf, Interface intf) {
    if (ipAddress != null) {
      l3Topology._ipOwners.add(ipAddress.getIp(), vrf);
      l3Topology._prefixOwners.add(ipAddress.toPrefix(), vrf);
      l3Topology._prefixOwners.add(ipAddress.toPrefix32(), vrf);

      l3Topology._ipv4Owners.add(ipAddress.getIp(), intf);
      l3Topology._ipv4Owners.add(ipAddress.toPrefix(), vrf, "interfaceAddress");
    }
  }
}
