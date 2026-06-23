package application.info;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.bgp.ISP;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.ipv4.Ip;
import datamodel.ipv4.PrefixRange;
import datamodel.routepolicy.RoutePolicy;
import datamodel.routepolicy.match.MatchFilterList;
import main.Controller;
import util.Ipv4Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static util.Ipv4Util.getPrefixRange;
import static util.Ipv4Util.getPrefixRange32;

/** Helper functions to collect statistics of configurations. */
public class ConfigInfo {
  public static Path OUTPUT;

  public static String getBgpPeerConfigInfo(BgpPeerConfig peerConfig) {
    String description = peerConfig.getDescription();
    description = description.replace("x", "");
    description = description.replace("*", "");
    return description.equals("") ? peerConfig.getRemoteIp().toString() : description;
  }

  public static void collectIpPrefixListUsageInfo(Router router) {
    Map<String, FilterList> map = router.getFilterLists().rowMap().get(FilterType.PREFIX);
    Multimap<String, String> usageMap = HashMultimap.create();

    for (VirtualRouter vrf : router.getVirtualRouters().values()) {
      if (vrf.getBgpProcess() == null) continue;
      vrf.getBgpProcess()
          .getNeighbors()
          .forEach(
              peerConfig -> {
                String appendix = getBgpPeerConfigInfo(peerConfig);
                FilterList imList =
                    peerConfig.getIpv4UnicastAddressFamily().getImportPrefixFilter();
                if (imList != null) {
                  if (map.containsKey(imList.getName())) {
                    usageMap.put(imList.getName(), String.join("\t", "[import list]", appendix));
                  }
                }
                FilterList exList =
                    peerConfig.getIpv4UnicastAddressFamily().getExportPrefixFilter();
                if (exList != null) {
                  if (map.containsKey(exList.getName())) {
                    usageMap.put(exList.getName(), String.join("\t", "[export list]", appendix));
                  }
                }
                RoutePolicy imPolicy =
                    peerConfig.getIpv4UnicastAddressFamily().getImportRoutePolicy();
                if (imPolicy != null) {
                  imPolicy
                      .getNodes()
                      .values()
                      .forEach(
                          node ->
                              node.getMatchAll()
                                  .getMatchOnes()
                                  .forEach(
                                      matchOne ->
                                          matchOne
                                              .getMatches()
                                              .forEach(
                                                  match -> {
                                                    if (match instanceof MatchFilterList) {
                                                      String name =
                                                          ((MatchFilterList) match).getListName();
                                                      if (map.containsKey(name)) {
                                                        usageMap.put(
                                                            name,
                                                            String.join(
                                                                "\t",
                                                                "[import policy]",
                                                                imPolicy.getName(),
                                                                appendix));
                                                      }
                                                    }
                                                  })));
                }
                RoutePolicy exPolicy =
                    peerConfig.getIpv4UnicastAddressFamily().getExportRoutePolicy();
                if (exPolicy != null) {
                  exPolicy
                      .getNodes()
                      .values()
                      .forEach(
                          node ->
                              node.getMatchAll()
                                  .getMatchOnes()
                                  .forEach(
                                      matchOne ->
                                          matchOne
                                              .getMatches()
                                              .forEach(
                                                  match -> {
                                                    if (match instanceof MatchFilterList) {
                                                      String name =
                                                          ((MatchFilterList) match).getListName();
                                                      if (map.containsKey(name)) {
                                                        usageMap.put(
                                                            name,
                                                            String.join(
                                                                "\t",
                                                                "[export policy]",
                                                                exPolicy.getName(),
                                                                appendix));
                                                      }
                                                    }
                                                  })));
                }
              });
    }

    try {
      BufferedWriter bw =
          new BufferedWriter(new FileWriter(OUTPUT.resolve(router.getRouterName()) + ".txt"));
      for (Map.Entry<String, Collection<String>> entry : usageMap.asMap().entrySet()) {
        List<String> list = entry.getValue().stream().sorted().collect(Collectors.toList());
        bw.write(entry.getKey());
        bw.write("\n\t");
        bw.write(String.join("\n\t", list));
        bw.write("\n");
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void collectIpPrefixListUsageInfo() {
    Controller.cpExecutor.bgpTopology.getRouterISPMultimap().keySet().stream()
        .map(VirtualRouter::getRouter)
        .distinct()
        .forEach(ConfigInfo::collectIpPrefixListUsageInfo);
  }

  public static boolean collectIntfAddr = false;

  /** Collect internal prefixes. todo: may need adjustment. */
  public static Set<PrefixRange> collectInternalPrefixes() {
    Set<PrefixRange> result = new HashSet<>();
    if (collectIntfAddr) result.addAll(collectInterfaceAddresses());
    result.addAll(collectBgpNetworks());
    result.addAll(collectInternalIpPrefixLists());
    result =
        result.stream()
            .filter(prefixRange -> prefixRange.getPrefix().getIp().asLong() != 0)
            .collect(Collectors.toSet());
    return result;
  }

  /** Collect interface subnets as internal prefixes. */
  public static Set<PrefixRange> collectInterfaceAddresses() {
    HashSet<PrefixRange> result = new HashSet<>();
    Controller.cpExecutor
        .l3Topology
        .getInterfaces()
        .values()
        .forEach(
            intf ->
                intf.getAllIpAddresses()
                    .forEach(
                        ipAddr -> {
                          result.add(getPrefixRange(ipAddr));
                          result.add(getPrefixRange32(ipAddr));
                        }));
    return result;
  }

  /** Collect prefixes announced by bgp network command as internal prefixes. */
  public static Set<PrefixRange> collectBgpNetworks() {
    HashSet<PrefixRange> result = new HashSet<>();
    Controller.cpExecutor
        .l3Topology
        .getNodes()
        .values()
        .forEach(
            router ->
                router
                    .getVirtualRouters()
                    .values()
                    .forEach(
                        vrf -> {
                          BgpRoutingProcess bgpProcess = vrf.getBgpProcess();
                          if (bgpProcess != null) {
                            bgpProcess
                                .getNetworks()
                                .values()
                                .forEach(
                                    networks ->
                                        result.addAll(
                                            networks.stream()
                                                .map(Ipv4Util::getPrefixRange)
                                                .collect(Collectors.toSet())));
                          }
                        }));
    return result;
  }

  /**
   * Peering routers connecting to external routers are mostly configured with ip-prefix lists to
   * prevent internal prefixes being imported from external routers. We collect those prefixes in
   * the import filters as internal prefixes.
   */
  public static Set<PrefixRange> collectInternalIpPrefixLists() {
    HashSet<PrefixRange> result = new HashSet<>();
    Controller.cpExecutor
        .bgpTopology
        .getRouterISPMultimap()
        .asMap()
        .forEach(
            ((vrf, isps) -> {
              Set<Ip> set =
                  isps.stream()
                      .filter(
                          isp ->
                              isp.getAs() == 65100 || isp.getAs() == 65140 || isp.getAs() == 65200)
                      .map(ISP::getIp)
                      .collect(Collectors.toSet());
              if (!set.isEmpty()) {
                BgpRoutingProcess bgpProcess = vrf.getBgpProcess();
                if (bgpProcess != null) {
                  bgpProcess.getNeighbors().stream()
                      .filter(peerConfig -> set.contains(peerConfig.getRemoteIp()))
                      .forEach(
                          peerConfig -> {
                            FilterList imList =
                                peerConfig.getIpv4UnicastAddressFamily().getImportPrefixFilter();
                            if (imList != null) {
                              result.addAll(imList.collectPermits());
                            }
                            RoutePolicy imPolicy =
                                peerConfig.getIpv4UnicastAddressFamily().getImportRoutePolicy();
                            if (imPolicy != null) {
                              imPolicy.getNodes().values().stream()
                                  .flatMap(
                                      node ->
                                          node.getMatchAll().getMatchOnes().stream()
                                              .flatMap(matchOne -> matchOne.getMatches().stream()))
                                  .filter(match -> match instanceof MatchFilterList)
                                  .map(
                                      match ->
                                          vrf.getRouter()
                                              .getFilterList(
                                                  FilterType.PREFIX,
                                                  ((MatchFilterList) match).getListName()))
                                  .filter(Objects::nonNull)
                                  .forEach(list -> result.addAll(list.collectPermits()));
                            }
                          });
                }
              }
            }));
    return result;
  }

  public static int[] countNumbers() {
    int numFilterLists =
        Controller.cpExecutor.l3Topology.getNodes().values().stream()
            .mapToInt(node -> node.getFilterLists().values().size())
            .sum();
    int numRoutePolicies =
        Controller.cpExecutor.l3Topology.getNodes().values().stream()
            .mapToInt(node -> node.getRoutePolicies().values().size())
            .sum();
    return new int[] {numFilterLists, numRoutePolicies};
  }
}
