package controlplane.process.bgp;

import com.google.common.collect.*;
import controlplane.network.Interface;
import controlplane.network.L3Topology;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import datamodel.aspath.AsPath;
import datamodel.community.Community;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import dataplane.fib.FibEntry;
import inputparser.bfvi.PeerIpParser;
import javafx.util.Pair;
import main.Controller;
import main.ExpressoLogger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class BgpTopology {
  TreeSet<Long> internalASNs;
  TreeSet<Prefix> internalPrefixes;

  Table<BgpRoutingProcess, BgpPeerConfig, BgpSessionProperties> edges;
  Multimap<ISP, BgpPeerConfig> ispPeers;
  Multimap<VirtualRouter, ISP> routerISPMultimap;

  public BgpTopology(TreeSet<Long> internalASNs, TreeSet<Prefix> internalPrefixes) {
    this.internalASNs = internalASNs;
    this.internalPrefixes = internalPrefixes;
    this.edges = HashBasedTable.create();
    this.ispPeers = HashMultimap.create();
    this.routerISPMultimap = HashMultimap.create();
  }

  public TreeSet<Long> getInternalASNs() {
    return internalASNs;
  }

  public TreeSet<Prefix> getInternalPrefixes() {
    return internalPrefixes;
  }

  public Table<BgpRoutingProcess, BgpPeerConfig, BgpSessionProperties> getEdges() {
    return edges;
  }

  public Collection<ISP> getISPs() {
    return ispPeers.asMap().keySet();
  }

  public Multimap<ISP, BgpPeerConfig> getISPPeers() {
    return ispPeers;
  }

  public Multimap<VirtualRouter, ISP> getRouterISPMultimap() {
    return routerISPMultimap;
  }

  Set<Interface> edgeInterfaces = null;

  public Set<Interface> getEdgeInterfaces() {
    if (edgeInterfaces == null) {
      edgeInterfaces =
          routerISPMultimap.asMap().entrySet().stream()
              .flatMap(
                  e -> {
                    VirtualRouter vrf = e.getKey();
                    Collection<ISP> col = e.getValue();
                    return col.stream()
                        .flatMap(
                            isp ->
                                ispPeers.asMap().get(isp).stream()
                                    .map(
                                        cfg -> {
                                          Interface edgeIf = null;
                                          if (cfg.getConnectInterface() != null)
                                            edgeIf = cfg.getConnectInterface();
                                          if (cfg.localIp != null) {
                                            for (Interface intf : vrf.getInterfaces().values()) {
                                              if (intf.getAllIpAddresses().stream()
                                                  .anyMatch(
                                                      addr ->
                                                          addr.toPrefix().ipMatch(cfg.localIp))) {
                                                edgeIf = intf;
                                                break;
                                              }
                                            }
                                          }
                                          return edgeIf != null && !edgeIf.isLoopback()
                                              ? edgeIf
                                              : null;
                                        }));
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      // ad hoc filtering
      if (edgeInterfaces.stream()
          .anyMatch(intf -> intf.getRouter().getRouterName().contains("pr"))) {
        edgeInterfaces.removeIf(intf -> !intf.getRouter().getRouterName().contains("pr"));
      }
    }
    return edgeInterfaces;
  }

  /** Build bgp topology considering dual direction packet reachability. */
  public void buildDualPacketReachability(L3Topology l3Topology) {
    // todo build bgp topology considering dual direction packet reachability.
    Multimap<Pair<Ip, Long>, BgpPeerConfig> tmp = HashMultimap.create();
    l3Topology
        .getNodes()
        .values()
        .forEach(
            router ->
                router
                    .getVirtualRouters()
                    .values()
                    .forEach(
                        localVrf -> {
                          BgpRoutingProcess localBgpProcess = localVrf.getBgpProcess();
                          if (localBgpProcess != null) {
                            for (BgpPeerConfig localConfig : localBgpProcess.neighbors) {
                              if (!shouldEstablish(localConfig)) {
                                continue;
                              }

                              Ip localIp = localConfig.localIp;
                              Ip remoteIp = localConfig.remoteIp;

                              // check local ip
                              if (localIp == null) {
                                localIp = resolveLocalIp(localConfig);
                                if (localIp == null) {
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.DEBUG,
                                      String.format("Local ip is null! %s", localConfig));
                                  continue;
                                }
                              }

                              // check remote ip
                              Collection<VirtualRouter> remoteVrfs =
                                  l3Topology.getIpOwners().get(remoteIp).stream()
                                      .filter(vrf -> checkRouteTarget(vrf, localVrf))
                                      .collect(Collectors.toSet());

                              if (remoteVrfs.size() == 0) {
                                if (isExternal(localConfig)) {
                                  localConfig.setPeerType(BgpPeerConfig.PeerType.EBGP);
                                  tmp.put(
                                      new Pair<>(localConfig.remoteIp, localConfig.remoteAS),
                                      localConfig);
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.DEBUG,
                                      String.format("Adding isp peer for %s", localConfig));
                                } else {
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.DEBUG,
                                      String.format("Ip %s has no owners", remoteIp));
                                }
                              } else {
                                HashSet<Router> remoteRouters = new HashSet<>();
                                for (VirtualRouter remoteVrf : remoteVrfs) {
                                  BgpRoutingProcess remoteBgpProcess = remoteVrf.getBgpProcess();
                                  if (remoteBgpProcess != null) {
                                    BgpPeerConfig remoteConfig =
                                        getBgpPeerConfig(remoteBgpProcess, localIp);
                                    if (shouldEstablish(remoteConfig)) {
                                      // check dual direction route reachability
                                      if (checkDualRouteReachability(
                                          localVrf, remoteVrf, localIp, remoteIp)) {
                                        setPeerType(localConfig, remoteConfig);
                                        addEdge(localConfig, remoteConfig);
                                        remoteRouters.add(remoteVrf.getRouter());
                                      } else {
                                        ExpressoLogger.log(
                                            ExpressoLogger.LEVEL.DEBUG,
                                            String.format(
                                                "Don't have route reachability for local config %s or remote config %s",
                                                localConfig, remoteConfig));
                                      }
                                    }
                                  }
                                }
                                if (remoteRouters.size() > 1) {
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.ERROR,
                                      String.format("Ip %s has more than one owners", remoteIp));
                                }
                              }
                            }
                          }
                        }));
    tmp.asMap()
        .forEach(
            (pair, configs) -> {
              if (canAddISP(
                  pair.getValue(),
                  configs.stream()
                      .map(BgpPeerConfig::getDescription)
                      .collect(Collectors.joining(" ")))) {
                ISP isp = BgpExternalPeer.generateIsp(pair.getKey(), pair.getValue(), configs);
                ispPeers.putAll(isp, configs);
                configs.stream()
                    .map(config -> config.process)
                    .collect(Collectors.toSet())
                    .forEach(p -> routerISPMultimap.put(p.virtualRouter, isp));
                Controller.bddManager.getBddEnvcWrapper().addIspVar(isp);
              }
            });
  }

  /**
   * Build bgp topology considering dual direction route reachability. This implementation is
   * consistent with how Batfish computes bgp topology. See {@link
   * org.batfish.datamodel.bgp.BgpTopologyUtils#initBgpTopology}.
   */
  public void buildDualRouteReachability(L3Topology l3Topology) {
    // fix the chosen ISPs when consider bounded number of ISPs
    Multimap<Pair<Ip, Long>, BgpPeerConfig> tmp =
        TreeMultimap.create(
            Comparator.comparing(Pair::getKey), Comparator.comparing(BgpPeerConfig::getRemoteIp));

    l3Topology
        .getNodes()
        .values()
        .forEach(
            router ->
                router
                    .getVirtualRouters()
                    .values()
                    .forEach(
                        localVrf -> {
                          BgpRoutingProcess localBgpProcess = localVrf.getBgpProcess();
                          if (localBgpProcess != null) {
                            for (BgpPeerConfig localConfig : localBgpProcess.neighbors) {
                              if (!shouldEstablish(localConfig)) continue;

                              Ip remoteIp = localConfig.remoteIp;
                              // check remote ip
                              Collection<VirtualRouter> remoteVrfs =
                                  l3Topology.getIpOwners().get(remoteIp).stream()
                                      .filter(vrf -> checkRouteTarget(vrf, localVrf))
                                      .collect(Collectors.toSet());

                              if (remoteVrfs.size() == 0) {
                                if (isExternal(localConfig)) {
                                  localConfig.setPeerType(BgpPeerConfig.PeerType.EBGP);
                                  tmp.put(new Pair<>(remoteIp, localConfig.remoteAS), localConfig);
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.DEBUG,
                                      String.format("Adding isp peer for %s", localConfig));
                                } else {
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.DEBUG,
                                      String.format(
                                          "Peer Ip %s on %s has no owners",
                                          remoteIp, localVrf.getFullName()));
                                }
                              } else {
                                HashSet<Router> remoteRouters = new HashSet<>();
                                for (VirtualRouter remoteVrf : remoteVrfs) {

                                  BgpRoutingProcess remoteBgpProcess = remoteVrf.getBgpProcess();
                                  if (remoteBgpProcess == null) continue;

                                  for (BgpPeerConfig remoteConfig : remoteBgpProcess.neighbors) {
                                    if (!shouldEstablish(remoteConfig)) continue;
                                    if (checkConsistency(localConfig, remoteConfig)) {
                                      // check dual direction route reachability
                                      if (checkDualRouteReachability(
                                          localVrf,
                                          remoteVrf,
                                          remoteConfig.remoteIp,
                                          localConfig.remoteIp)) {
                                        setPeerType(localConfig, remoteConfig);
                                        addEdge(localConfig, remoteConfig);
                                        remoteRouters.add(remoteVrf.getRouter());
                                      } else {
                                        ExpressoLogger.log(
                                            ExpressoLogger.LEVEL.DEBUG,
                                            String.format(
                                                "Don't have route reachability for local config %s or remote config %s",
                                                localConfig, remoteConfig));
                                      }
                                    } else {
                                      ExpressoLogger.log(
                                          ExpressoLogger.LEVEL.DEBUG,
                                          String.format(
                                              "Configs are inconsistent for %s and %s",
                                              localConfig, remoteConfig));
                                    }
                                  }
                                }
                                if (remoteRouters.size() > 1) {
                                  ExpressoLogger.log(
                                      ExpressoLogger.LEVEL.ERROR,
                                      String.format("Ip %s has more than one owners", remoteIp));
                                }
                              }
                            }
                          }
                        }));
    addIsps(tmp);
  }

  private void addIsps(Multimap<Pair<Ip, Long>, BgpPeerConfig> candidates) {
    if (STRATEGY == IspStrategy.BOUNDED_NUMBER_OF_ISP_RANDOM) {
      if (candidates.size() <= N_ISP) {
        candidates.asMap().forEach(this::addIsp);
      } else {
        Random rand = new Random();
        HashSet<Integer> hs = new HashSet<>();
        while (hs.size() < N_ISP) {
          hs.add(rand.nextInt(candidates.asMap().entrySet().size()));
        }
        int i = 0;
        for (Map.Entry<Pair<Ip, Long>, Collection<BgpPeerConfig>> entry :
            candidates.asMap().entrySet()) {
          if (hs.contains(i)) {
            addIsp(entry.getKey(), entry.getValue());
          }
          i++;
        }
      }
    } else {
      candidates
          .asMap()
          .forEach(
              (pair, configs) -> {
                if (canAddISP(
                    pair.getValue(),
                    configs.stream()
                        .map(BgpPeerConfig::getDescription)
                        .collect(Collectors.joining(" ")))) {
                  addIsp(pair, configs);
                }
              });
    }

    Collection<ISP> isps = getISPs();
    if (isps.size() < candidates.asMap().keySet().size()) {
      String str = isps.stream().map(ISP::toString).sorted().collect(Collectors.joining(", "));
      ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, str);
    }

    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "[NUM ISP] " + isps.size());
  }

  private void addIsp(Pair<Ip, Long> pair, Collection<BgpPeerConfig> configs) {
    ISP isp = BgpExternalPeer.generateIsp(pair.getKey(), pair.getValue(), configs);
    ispPeers.putAll(isp, configs);
    configs.stream()
        .map(config -> config.process)
        .collect(Collectors.toSet())
        .forEach(p -> routerISPMultimap.put(p.virtualRouter, isp));
    Controller.bddManager.getBddEnvcWrapper().addIspVar(isp);
  }

  // todo: may need adjustment
  private boolean isExternal(BgpPeerConfig localConfig) {
    if (isBagpipePeers(localConfig.remoteIp)) {
      return true;
    }
    return localConfig.remoteAS != localConfig.localAS
        && localConfig.remoteAS != 55990
        && localConfig.remoteAS != 100
        && !AsPath.isPrivateAs(localConfig.remoteAS);
  }

  private TreeSet<Ip> bagpipePeerIps;

  private boolean isBagpipePeers(Ip remoteIp) {
    if (bagpipePeerIps == null) bagpipePeerIps = PeerIpParser.parsePeerIps(null);
    return bagpipePeerIps.contains(remoteIp);
  }

  public enum IspStrategy {
    NO_ISP,
    ALL,
    APPOINTED_ISP,
    BOUNDED_NUMBER_OF_ISP,
    BOUNDED_NUMBER_OF_ISP_RANDOM,
    BOUNDED_NUMBER_OF_SMALL_ISP
  }

  private IspStrategy STRATEGY = IspStrategy.ALL;

  public void setIspStrategy(
      IspStrategy strategy,
      @Nullable Set<Long> appointed,
      @Nullable Integer nIsp,
      @Nullable Integer nSmallIsp) {
    STRATEGY = strategy;
    if (strategy == IspStrategy.APPOINTED_ISP) {
      APPOINTED_ISPS = firstNonNull(appointed, Collections.emptySet());
    }
    if (strategy == IspStrategy.BOUNDED_NUMBER_OF_ISP
        || strategy == IspStrategy.BOUNDED_NUMBER_OF_ISP_RANDOM) {
      N_ISP = firstNonNull(nIsp, Integer.MAX_VALUE);
      this.nIsp = 0;
    }
    if (strategy == IspStrategy.BOUNDED_NUMBER_OF_SMALL_ISP) {
      N_SMALL_ISP = firstNonNull(nSmallIsp, Integer.MAX_VALUE);
      this.nSmallIsp = 0;
    }
  }

  public boolean canAddISP(long asn, String description) {
    switch (STRATEGY) {
      case NO_ISP:
        return false;
      case ALL:
        return true;
      case APPOINTED_ISP:
        return appointedIsp(asn);
      case BOUNDED_NUMBER_OF_ISP:
        return boundedNumberOfIsp();
      case BOUNDED_NUMBER_OF_SMALL_ISP:
        return boundedNumberOfSmallIsp(description);
    }
    return false;
  }

  private Set<Long> APPOINTED_ISPS = Collections.emptySet();

  private boolean appointedIsp(long asn) {
    return APPOINTED_ISPS.contains(asn);
  }

  private int N_ISP = 500;
  private int nIsp = 0;

  private boolean boundedNumberOfIsp() {
    if (nIsp < N_ISP) {
      nIsp++;
      return true;
    }
    return false;
  }

  private int N_SMALL_ISP = 500;
  private int nSmallIsp = 0;

  private boolean boundedNumberOfSmallIsp(String description) {
    if (description.toLowerCase().contains("telecom")
        || description.toLowerCase().contains("unicom")
        || description.toLowerCase().contains("mobile")
        || description.toLowerCase().contains("ct")
        || description.toLowerCase().contains("cu")
        || description.toLowerCase().contains("cmcc")) {
      return true;
    }
    if (nSmallIsp < N_SMALL_ISP) {
      nSmallIsp++;
      return true;
    }
    return false;
  }

  private void addEdge(BgpPeerConfig cfg1, BgpPeerConfig cfg2) {
    // first we finalize incomplete peer configs
    if (cfg1.localIp == null) {
      cfg1.localIp = cfg2.remoteIp;
    }
    if (cfg2.localIp == null) {
      cfg2.localIp = cfg1.remoteIp;
    }
    // then we add edges to bgp topology
    if (!edges.contains(cfg1.process, cfg1)) {
      edges.put(cfg1.process, cfg1, new BgpSessionProperties(cfg1, cfg2));
    }
    if (!edges.contains(cfg2.process, cfg2)) {
      edges.put(cfg2.process, cfg2, new BgpSessionProperties(cfg2, cfg1));
    }
  }

  /** check whether this peer has been disabled or ignored */
  public static boolean shouldEstablish(BgpPeerConfig config) {
    return config != null && config.ipv4UnicastAddressFamily.isEnable() && !config.ignore;
  }

  /**
   * Commonly, <br>
   * 1. IBGP peers use loopback interfaces to establish a neighbor adjacency. <br>
   * 2. EBGP peers are directly connected to each other to establish a neighbor adjacency. This is
   * because EBGP routers use a <b>TTL of one</b> for their BGP packets. <br>
   * Therefore, if two BGP routers that are not directly connected want to establish an EBGP
   * neighbor adjacency, they have to be configured ebgp-multihop (on Cisco devices) or ebgp-max-hop
   * (on Huawei devices). This requirement does not apply to internal BGP. <br>
   * If you don't configure local-address, BGP uses the routing device's source address selection
   * rules to set the local address, which normally result in the egress interface address.
   */
  public static Ip resolveLocalIp(BgpPeerConfig cfg) {
    if (cfg.localIp == null) {
      VirtualRouter localVrf = cfg.process.virtualRouter;
      Optional<Interface> optional =
          localVrf.getInterfaces().values().stream()
              .filter(intf -> intf.getPrimaryIpAddress() != null)
              .filter(intf -> intf.getPrimaryIpAddress().toPrefix().ipMatch(cfg.remoteIp))
              .findFirst();
      optional.ifPresent(anInterface -> cfg.localIp = anInterface.getIp());
      if (optional.isEmpty() && cfg.ebgpMaxHop > 1) {
        FibEntry fibEntry = localVrf.getFib().longestPrefixMatch(cfg.remoteIp);
        if (fibEntry != null) {
          if (fibEntry.getNextHopInterface() != null) {
            cfg.localIp = fibEntry.getNextHopInterface().getIp();
          }
        }
      }
    }
    return cfg.localIp;
  }

  private static BgpPeerConfig getBgpPeerConfig(BgpRoutingProcess bgpProcess, Ip remoteIp) {
    for (BgpPeerConfig cfg : bgpProcess.neighbors) {
      if (cfg.remoteIp != null && cfg.remoteIp.equals(remoteIp)) {
        return cfg;
      }
    }
    return null;
  }

  private static boolean checkConsistency(BgpPeerConfig cfg1, BgpPeerConfig cfg2) {
    return checkAsnConsistency(cfg1, cfg2) && checkIpConsistency(cfg1, cfg2);
  }

  private static boolean checkAsnConsistency(BgpPeerConfig cfg1, BgpPeerConfig cfg2) {
    return cfg1.localAS == cfg2.remoteAS && cfg1.remoteAS == cfg2.localAS;
  }

  private static boolean checkIpConsistency(BgpPeerConfig cfg1, BgpPeerConfig cfg2) {
    //        return cfg1.localIp == cfg2.remoteIp && cfg1.remoteIp == cfg2.localIp;
    // From Batfish: If cfg2 has no local IP, we can still initiate unless session is EBGP
    // single-hop
    return (Objects.equals(cfg1.remoteIp, cfg2.localIp)
            || (cfg2.localIp == null && !(cfg1.external && cfg1.ebgpMaxHop <= 1)))
        && Objects.equals(cfg1.localIp, cfg2.remoteIp);
  }

  private static boolean checkDualRouteReachability(
      VirtualRouter localVrf, VirtualRouter remoteVrf, Ip localIp, Ip remoteIp) {
    return localVrf.getGlobalRib().longestPrefixMatch(remoteIp) != null
        && remoteVrf.getGlobalRib().longestPrefixMatch(localIp) != null;
  }

  private static void setPeerType(BgpPeerConfig cfg1, BgpPeerConfig cfg2) {
    if (cfg1.localAS != cfg1.remoteAS) {
      cfg1.setPeerType(BgpPeerConfig.PeerType.EBGP);
      cfg2.setPeerType(BgpPeerConfig.PeerType.EBGP);
    } else {
      if (cfg1.ipv4UnicastAddressFamily.isRouteReflectorClient()) {
        cfg1.setPeerType(BgpPeerConfig.PeerType.CLIENT);
        cfg2.setPeerType(BgpPeerConfig.PeerType.REFLECTOR);
      } else if (cfg2.ipv4UnicastAddressFamily.isRouteReflectorClient()) {
        cfg1.setPeerType(BgpPeerConfig.PeerType.REFLECTOR);
        cfg2.setPeerType(BgpPeerConfig.PeerType.CLIENT);
      } else {
        cfg1.setPeerType(BgpPeerConfig.PeerType.IBGP);
        cfg2.setPeerType(BgpPeerConfig.PeerType.IBGP);
      }
    }
  }

  private static boolean checkRouteTarget(VirtualRouter vrf1, VirtualRouter vrf2) {
    Set<Community> iRT1 = new HashSet<>(vrf1.getiRTs());
    Set<Community> iRT2 = new HashSet<>(vrf2.getiRTs());

    // at least one of vrf1 and vrf2 is a default vrf
    if (iRT1.isEmpty() || iRT2.isEmpty()) {
      return true;
    }

    iRT1.retainAll(vrf2.geteRTs());
    iRT2.retainAll(vrf1.geteRTs());
    return !iRT1.isEmpty() && !iRT2.isEmpty();
  }
}
