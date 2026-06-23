package controlplane.process.bgp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import controlplane.network.VirtualRouter;
import controlplane.process.Process;
import controlplane.process.RedistributionConfig;
import controlplane.process.bgp.routeaggregation.RouteAggregation;
import controlplane.process.bgp.routeaggregation.RouteAggregationResult;
import controlplane.rib.BgpRib;
import controlplane.rib.RecursiveResolver;
import controlplane.rib.Rib;
import controlplane.rib.RibRouteSet;
import controlplane.route.BgpRoute;
import controlplane.route.Route;
import controlplane.route.RoutingProtocol;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.aspath.AsSet;
import datamodel.community.CommunityListFactory;
import datamodel.community.CommunityListIntf;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.path.BTE;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.RoutePolicy;
import javafx.util.Pair;
import main.Controller;
import main.ExpressoLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class BgpRoutingProcess implements Process {
  VirtualRouter virtualRouter;
  Ip routerId;
  HashSet<BgpPeerConfig> neighbors;
  HashMap<BgpPeerConfig, RouteSet<BgpRoute>> knownExternalRoutes;
  HashMap<RoutePolicy, HashSet<Prefix>> networks;

  /**
   * Protocol preference for EBGP, IBGP and local BGP routes. It affects route selection among BGP
   * routes and routes of other routing protocols.</br>
   *
   * <p>By default, the protocol preferences of EBGP routes, IBGP routes, and local BGP routes are
   * all 255. The values are integers that range from 1 to 255. The smaller a value is, the higher
   * its preference is. </br>
   *
   * <p>Local BGP routes are routes summarized by using the <b>summary automatic</b> command or the
   * <b>aggregate</b> command.
   */
  int[] preference;

  int defaultLocalPreference;

  boolean additionalPath;
  boolean iBgpMultipath;
  boolean eBgpMultipath;

  /** route redistribution */
  HashSet<RedistributionConfig> imports;

  /** route aggregation */
  Vector<RouteAggregation> aggregations;

  /**
   * queues for route propagation <br>
   * todo make it consistent to <a
   * href="https://www.rfc-editor.org/rfc/rfc4271#section-3.2">RFC4271</a> <br>
   * Adj-RIBs-In: contains unprocessed routing information that has been advertised to the local BGP
   * speaker by its peers. <br>
   * Loc-RIB: contains the routes that have been selected by the local BGP speaker's Decision
   * Process. <br>
   * Adj-RIBs-Out: organizes the routes for advertisement to specific peers (by means of the local
   * speaker's UPDATE message). <br>
   *
   * <p>Using {@link RouteSet} here because we need to combine routes with the same attributes.
   */
  // ArrayList<BgpRoute> inQueue = new ArrayList<>();
  RouteSet<BgpRoute> inQueue = new RouteSet<>();

  /**
   * Using {@link RibRouteSet} here because we need to let the new route replace the old route that
   * has the same attributes. By "replacing", we are combining the process of first withdraw an old
   * route then install a new route.
   */
  RibRouteSet<BgpRoute> allIncomingRoutes = new RibRouteSet<>();

  BgpRib additionalPathBgpRib;
  BgpRib multipathBgpRib;

  /** Using {@link RouteSet} here because we need to combine routes with the same attributes. */
  RouteSet<BgpRoute> additionalPathOutQueue = new RouteSet<>();

  RouteSet<BgpRoute> multipathOutQueue = new RouteSet<>();

  /** border router? route reflector? */
  Boolean br;

  Boolean rr;

  public BgpRoutingProcess(
      VirtualRouter virtualRouter,
      Ip routerId,
      HashSet<BgpPeerConfig> neighbors,
      HashMap<RoutePolicy, HashSet<Prefix>> networks,
      int[] preference,
      int defaultLocalPreference,
      int maxLoadBalancingIbgp,
      int maxLoadBalancingEbgp,
      HashSet<RedistributionConfig> imports,
      Vector<RouteAggregation> aggregations) {
    this.virtualRouter = virtualRouter;
    this.routerId = routerId;
    this.neighbors = firstNonNull(neighbors, new HashSet<>());
    this.networks = networks;
    this.preference = preference;
    this.defaultLocalPreference = defaultLocalPreference;
    this.imports = imports;
    this.aggregations = aggregations;

    this.neighbors.forEach(neighbor -> neighbor.setProcess(this));
    this.additionalPath =
        neighbors.stream()
            .anyMatch(
                neighbor ->
                    neighbor
                        .ipv4UnicastAddressFamily
                        .getAddressFamilyCapabilities()
                        .isAdditionalPathsSend());
    this.iBgpMultipath = maxLoadBalancingIbgp > 1;
    this.eBgpMultipath = maxLoadBalancingEbgp > 1;
    this.additionalPathBgpRib = new BgpRib(true, iBgpMultipath, eBgpMultipath);
    this.multipathBgpRib = new BgpRib(false, iBgpMultipath, eBgpMultipath);
  }

  public VirtualRouter getVrf() {
    return virtualRouter;
  }

  @Override
  public BgpRib getRib() {
    return multipathBgpRib;
  }

  public HashSet<BgpPeerConfig> getNeighbors() {
    return neighbors;
  }

  public void setKnownExternalRoutes(
      HashMap<BgpPeerConfig, RouteSet<BgpRoute>> knownExternalRoutes) {
    this.knownExternalRoutes = knownExternalRoutes;
  }

  public HashMap<RoutePolicy, HashSet<Prefix>> getNetworks() {
    return networks;
  }

  /**
   * generate original bgp routes configured by "network", "import-route" and "import-rib" command.
   */
  public void begin() {
    network();
    imports();
    //    importKnownExternalRoutes();
  }

  /** import routes from the global rib according to "network" commands. */
  private void network() {
    RouteSet<BgpRoute> shouldNetwork = new RouteSet<>();
    /* According to Cisco documentation, when using network command in BGP,
    we have to check whether the prefix is already in the router's RIB. */
    for (Map.Entry<RoutePolicy, HashSet<Prefix>> entry : networks.entrySet()) {
      Set<Prefix> prefixes =
          entry.getValue().stream()
              .filter(prefix -> virtualRouter.getGlobalRib().match(prefix))
              .collect(Collectors.toSet());

      if (!prefixes.isEmpty()) {
        BgpRoute bgpRoute =
            new BgpRouteBuilder()
                .setPrefixesBdd(
                    // only network prefixes we care about
                    Controller.bddManager.and(
                        Controller.bddManager.getBddPrefixWrapper().prefixSpaceBdd,
                        Controller.bddManager.getBddPrefixWrapper().encodePrefixes(prefixes)))
                // todo 255 or preference[2]?
                .setPreference(preference[2])
                .setLocalPreference(defaultLocalPreference)
                .setOrigin(BgpRoute.OriginType.IGP)
                .setCommunityList(CommunityListFactory.empty())
                .setType(BgpRoute.BgpRouteType.NETWORK)
                .setIgpCostToNextHop(0)
                .build();

        if (entry.getKey() == null) {
          //          insertRib(bgpRoute);
          shouldNetwork.add(bgpRoute);
        } else {
          RouteFilterResult<BgpRoute> result =
              entry
                  .getKey()
                  .filter(
                      bgpRoute,
                      new RouteFilterEnvironment.Builder<BgpRoute>()
                          .setIn(true)
                          .setLocalConfig(null)
                          .build());
          //          result.getPermitted().getAllRoutes().forEach(this::insertRib);
          result.getPermitted().getAllRoutes().forEach(shouldNetwork::add);
        }
      }
    }
    shouldNetwork.getAllRoutes().forEach(this::insertRib);
  }

  private List<? extends Route> getProtocolRib(
      VirtualRouter vrf, RoutingProtocol protocol, Integer processId) {
    // todo
    switch (protocol) {
      case CONNECTED:
        return vrf.getConnectedRib().getAllRoutes();
      case LOCAL:
        return vrf.getLocalRib().getAllRoutes();
      case STATIC:
        return vrf.getGlobalRib().getAllRoutes().stream()
            .filter(route -> route.getRoutingProtocol() == RoutingProtocol.STATIC)
            .collect(Collectors.toList());
      case OSPF:
      case OSPF_ASE:
      case OSPF_NSSA:
        return vrf.getOspfProcesses().get(processId) == null
            ? Collections.emptyList()
            : vrf.getOspfProcesses().get(processId).getRib().getAllRoutes();
      case BGP:
        return vrf.getBgpProcess() == null
            ? Collections.emptyList()
            : vrf.getBgpProcess().getRib().getAllRoutes();
      default:
        return Collections.emptyList();
    }
  }

  /**
   * import routes from other routing protocol according to "import-route" and "import-rib"
   * commands.
   */
  public void imports() {
    RouteSet<BgpRoute> shouldImport = new RouteSet<>();
    for (RedistributionConfig config : imports) {
      VirtualRouter srcVrf = config.getVrf() == null ? virtualRouter : config.getVrf();
      RoutingProtocol srcProtocol = config.getProtocol();
      Integer srcProcessId = config.getProcessId();
      List<? extends Route> routes = getProtocolRib(srcVrf, srcProtocol, srcProcessId);

      Long med = config.getMed();
      RoutePolicy routePolicy = config.getRoutePolicy();

      routes.forEach(
          route -> {
            // only import prefixes we care about
            int intersect =
                Controller.bddManager.and(
                    Controller.bddManager.getBddPrefixWrapper().prefixSpaceBdd,
                    route.getPrefixesBdd());
            if (intersect == 0) return;
            BgpRouteBuilder builder =
                new BgpRouteBuilder()
                    .setPrefixesBdd(intersect)
                    // todo 255 or preference[2]?
                    .setPreference(preference[2])
                    .setOrigin(BgpRoute.OriginType.INCOMPLETE)
                    .setCommunityList(CommunityListFactory.empty())
                    .setType(BgpRoute.BgpRouteType.IMPORT)
                    .setTag(route.getTag())
                    .setIgpCostToNextHop(route.getMetric());

            if (med != null) builder.setMed(med);
            BgpRoute bgpRoute = builder.build();

            if (routePolicy != null) {
              RouteFilterResult<BgpRoute> result =
                  routePolicy.filter(
                      bgpRoute,
                      new RouteFilterEnvironment.Builder<BgpRoute>()
                          .setIn(true)
                          .setLocalConfig(null)
                          .build());
              //              result.getPermitted().getAllRoutes().forEach(this::insertRib);
              result.getPermitted().getAllRoutes().forEach(shouldImport::add);
            } else {
              //              insertRib(bgpRoute);
              shouldImport.add(bgpRoute);
            }
          });
    }
    shouldImport.getAllRoutes().forEach(this::insertRib);
  }

  public void importKnownExternalRoutes() {
    if (knownExternalRoutes != null) {
      knownExternalRoutes.forEach(
          (peerConfig, bgpRoutes) ->
              bgpRoutes.getAllRoutes().forEach(bgpRoute -> routeIn(bgpRoute, peerConfig)));
    }
  }

  private void insertRib(BgpRoute bgpRoute) {
    multipathBgpRib.simpleInsert(bgpRoute);
    if (additionalPath) additionalPathBgpRib.simpleInsert(bgpRoute);
  }

  /**
   * check if there exists loop if so, drop else, insert it into the inQueue
   *
   * @param route the incoming route
   * @param localConfig the local {@link BgpPeerConfig} containing import/export filters and
   *     policies to the peer sending this route
   */
  public void routeIn(BgpRoute route, BgpPeerConfig localConfig) {
    routeIn(route, localConfig, false);
  }

  public void routeIn(BgpRoute route, BgpPeerConfig localConfig, boolean fromIsp) {
    RouteFilterResult<BgpRoute> result = localConfig.processIn(route);
    result
        .getPermitted()
        .getAllRoutes()
        .forEach(
            permitted -> {
              setPreference(permitted);
              if (fromIsp) {
                Pair<CommunityListIntf, CommunityListIntf> pair =
                    permitted.getCommunities().match(BTE.getCommunityRegex());
                if (pair.getKey() != null) {
                  BgpRoute bgpRoute =
                      permitted.toBuilder()
                          .setCommunityList(pair.getKey())
                          .setPath(permitted.getPath().append(BTE.INSTANCE))
                          .build();
                  inQueue.add(bgpRoute);
                }
                if (pair.getValue() != null) {
                  BgpRoute bgpRoute =
                      permitted.toBuilder().setCommunityList(pair.getValue()).build();
                  inQueue.add(bgpRoute);
                }
              } else {
                inQueue.add(permitted);
              }
            });
  }

  private void setPreference(BgpRoute route) {
    switch (route.getType()) {
      case FROM_EBGP_NEIGHBOR:
        route.setPreference(preference[0]);
        break;
      case FROM_IBGP_NEIGHBOR:
      case FROM_ROUTE_REFLECTOR:
      case FROM_CLIENT:
        route.setPreference(preference[1]);
        break;
      case AGGREGATE:
      case SUMMARY:
        route.setPreference(preference[2]);
        break;
      default:
        route.setPreference(255);
    }
  }

  public boolean updateRib() {
    // step 1. check the next hop of incoming routes.
    List<BgpRoute> resolved =
        inQueue.getAllRoutes().stream()
            .map(this::nexthopRecursiveLookup)
            // if the next hop address is unreachable, the route is ignored
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    inQueue.clear();

    if (aggregations.isEmpty()) {
      boolean converged = updateRib(multipathBgpRib, multipathOutQueue, resolved);
      if (additionalPath) {
        converged = updateRib(additionalPathBgpRib, additionalPathOutQueue, resolved) & converged;
      }

      return converged;
    } else {
      allIncomingRoutes.replaceAll(resolved);
      List<BgpRoute> incomingDelta = allIncomingRoutes.getDelta();
      allIncomingRoutes.clean();
      if (incomingDelta.isEmpty()) return true;

      List<RouteAggregationResult> results =
          aggregations.stream()
              .map(aggr -> aggr.process(allIncomingRoutes.getAllRoutes()))
              .collect(Collectors.toList());
      RouteAggregationResult result = RouteAggregationResult.combine(results);
      result.getAggrs().getAllRoutes().forEach(this::setPreference);

      boolean converged = updateRib(multipathBgpRib, multipathOutQueue, result);
      if (additionalPath) {
        converged = updateRib(additionalPathBgpRib, additionalPathOutQueue, result) & converged;
      }

      return converged;
    }
  }

  private boolean updateRib(
      Rib<BgpRoute> rib, RouteSet<BgpRoute> outQueue, List<BgpRoute> resolved) {
    // step 2. insert all incoming routes into rib
    rib.sortInsert(resolved);

    // step 3. get rib delta.
    rib.getDelta().stream().map(BgpRoute::prepareOut).forEach(outQueue::add);

    // step 4. clean rib.
    rib.clean();

    return outQueue.getAllRoutes().isEmpty();
  }

  private boolean updateRib(
      Rib<BgpRoute> rib, RouteSet<BgpRoute> outQueue, RouteAggregationResult result) {
    // step 2. insert all incoming routes into rib
    rib.simpleInsert(result.getSuppressed().getAllRoutes());
    rib.simpleInsert(result.getOthers().getAllRoutes());
    rib.sortInsert(result.getAggrs().getAllRoutes());

    // step 3. get rib delta.
    rib.getDelta().stream()
        .filter(bgpRoute -> !bgpRoute.isSuppressed())
        .map(BgpRoute::prepareOut)
        .forEach(outQueue::add);

    // step 4. clean rib.
    rib.clean();

    return outQueue.getAllRoutes().isEmpty();
  }

  public BgpRoute nexthopRecursiveLookup(BgpRoute bgpRoute) {
    if (bgpRoute.getNextHopInterface() != null) {
      ExpressoLogger.log(
          ExpressoLogger.LEVEL.DEBUG, "bgp route with next hop interface? " + bgpRoute);
      return bgpRoute.toBuilder().setIgpCostToNextHop(0).build();
    } else {
      // Until now, there are only IGP routes in the global RIB which don't have ENVC.
      // Therefore, we only care about whether the bgp route's next hop can be recursively resolved
      // or not.
      return RecursiveResolver.resolveRoute(bgpRoute, virtualRouter.getGlobalRib()).stream()
          .map(result -> Iterables.getLast(result.getResolutionSteps()))
          .map(Route::getMetric)
          .min(Long::compareTo)
          .map(aLong -> bgpRoute.toBuilder().setIgpCostToNextHop(aLong).build())
          .orElse(null);
    }
  }

  public void routeOut(
      Map<BgpPeerConfig, BgpSessionProperties> peers,
      Function<BgpSessionProperties, Boolean> func) {
    for (Map.Entry<BgpPeerConfig, BgpSessionProperties> peer : peers.entrySet()) {
      List<BgpRoute> routes =
          (peer.getKey()
                      .ipv4UnicastAddressFamily
                      .getAddressFamilyCapabilities()
                      .isAdditionalPathsSend()
                  ? additionalPathOutQueue
                  : multipathOutQueue)
              .getAllRoutes();
      BgpSessionProperties session = peer.getValue();
      for (BgpRoute route : routes) {
        // avoid propagation path loop
        if (!route.getPath().contains(session.remote.process.virtualRouter)
            && func.apply(session)) {
          propagate(route, session);
        }
      }
    }
    additionalPathOutQueue = new RouteSet<>();
    multipathOutQueue = new RouteSet<>();
  }

  /**
   * Propagate {@code route} along the {@code edge}. <br>
   * Only legal propagations are allowed. For example, if {@code route} is received from an IBGP
   * peer or RR (route reflector), then it can only be propagated to an EBGP peer or a client. <br>
   * Outbound route filters are applied by calling {@link BgpSessionProperties#local}'s {@link
   * BgpRoutingProcess#processOut(BgpRoute, BgpPeerConfig)}. <br>
   * Inbound route filters are applied by calling {@link BgpSessionProperties#remote}'s {@link
   * BgpRoutingProcess#routeIn(BgpRoute, BgpPeerConfig)}.
   */
  public static void propagate(BgpRoute route, BgpSessionProperties edge) {
    if (route.getType() == BgpRoute.BgpRouteType.FROM_IBGP_NEIGHBOR
        || route.getType() == BgpRoute.BgpRouteType.FROM_ROUTE_REFLECTOR) {
      // routes from ibgp peers or route reflector
      // only propagate to ebgp peers or route reflector clients
      if (!(edge.local.peerType == BgpPeerConfig.PeerType.EBGP
          || edge.local.peerType == BgpPeerConfig.PeerType.CLIENT)) {
        return;
      }
    }
    Collection<BgpRoute> permitOut = processOut(route, edge.local);
    permitOut.forEach(one -> edge.remote.process.routeIn(one, edge.remote));
  }

  /**
   * Apply out-bounding route filters and route policies and then update the attributes of the
   * permitted bgp routes.
   *
   * <p><a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1000097183?section=j0dl">huawei-doc</a>:
   * <br>
   * A BGP speaker processes the next hop attribute based on the following rules: <br>
   * 1. When advertising a route to an EBGP peer, a BGP speaker sets the next hop attribute of the
   * route to the address of the local interface through which the BGP peer relationship is
   * established with the peer. <br>
   * 2. When advertising a locally originated route to an IBGP peer, the BGP speaker sets the next
   * hop attribute of the route to the address of the local interface through which the BGP peer
   * relationship is established with the peer. <br>
   * 3. When advertising a route learned from an EBGP peer to an IBGP peer, the BGP speaker does not
   * change the next hop attribute of the route.
   */
  public static Collection<BgpRoute> processOut(BgpRoute route, BgpPeerConfig localConfig) {
    // apply export route policy
    RouteFilterResult<BgpRoute> result = localConfig.processOut(route.toBuilder().build());

    return result.getPermitted().getAllRoutes().stream()
        .map(
            bgpRoute -> {
              BgpRouteBuilder builder = bgpRoute.toBuilder();

              // set next hop ip
              if (localConfig.peerType == BgpPeerConfig.PeerType.EBGP
                  || bgpRoute.getType() == BgpRoute.BgpRouteType.NETWORK
                  || bgpRoute.getType() == BgpRoute.BgpRouteType.IMPORT) {
                builder.setNextHopIp(localConfig.localIp);
              }

              // set bgp route type
              builder.setType(localConfig.peerType.bgpRouteType);

              // extend propagation path
              builder.setPath(builder.getPath().append(localConfig.process.virtualRouter));

              // EBGP:
              // 1. Extend AS path
              // 2. Restore local preference to default value
              if (localConfig.peerType == BgpPeerConfig.PeerType.EBGP) {
                // extend as path
                builder.setAsPath(
                    builder
                        .getAsPath()
                        .append(Collections.singletonList(AsSet.of(localConfig.localAS))));

                // restore local preference
                builder.setLocalPreference(localConfig.process.defaultLocalPreference);
              }

              return builder.build();
            })
        .collect(Collectors.toList());
  }

  /** RRs send routes to their IR clients. */
  public void rrFinalRound(Map<BgpPeerConfig, BgpSessionProperties> peers) {
    Set<Map.Entry<BgpPeerConfig, BgpSessionProperties>> irClients;
    irClients =
        peers.entrySet().stream()
            .filter(entry -> entry.getValue().remote.process.isIR())
            .collect(Collectors.toSet());
    if (!irClients.isEmpty()) {
      irClients.forEach(
          entry -> {
            BgpRib rib;
            if (entry
                .getKey()
                .ipv4UnicastAddressFamily
                .getAddressFamilyCapabilities()
                .isAdditionalPathsSend()) {
              rib = additionalPathBgpRib;
            } else {
              rib = multipathBgpRib;
            }
            rib.getAllRoutes()
                .forEach(bgpRoute -> propagate(bgpRoute.prepareOut(), entry.getValue()));
          });
    }
  }

  public boolean isRR() {
    return rr =
        (rr != null
            ? rr
            : neighbors.stream()
                .anyMatch(neighbor -> neighbor.peerType == BgpPeerConfig.PeerType.CLIENT));
  }

  public boolean isBR() {
    return br =
        (br != null
            ? br
            : neighbors.stream()
                .anyMatch(neighbor -> neighbor.peerType == BgpPeerConfig.PeerType.EBGP));
  }

  public boolean isIR() {
    return !(isRR() || isBR());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("vrf", virtualRouter)
        .add("routerId", routerId)
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    VirtualRouter virtualRouter;
    Ip routerId;
    HashSet<BgpPeerConfig> neighbors;
    HashMap<RoutePolicy, HashSet<Prefix>> networks;
    int[] preference;
    int defaultLocalPreference;
    int maxLoadBalancingIbgp;
    int maxLoadBalancingEbgp;
    HashSet<RedistributionConfig> imports;
    Vector<RouteAggregation> aggregations;

    private Builder() {}

    public BgpRoutingProcess build() {
      return new BgpRoutingProcess(
          virtualRouter,
          routerId,
          neighbors,
          networks,
          preference,
          defaultLocalPreference,
          maxLoadBalancingIbgp,
          maxLoadBalancingEbgp,
          imports,
          aggregations);
    }

    public Builder setVirtualRouter(VirtualRouter virtualRouter) {
      this.virtualRouter = virtualRouter;
      return this;
    }

    public Builder setRouterId(Ip routerId) {
      this.routerId = routerId;
      return this;
    }

    public Builder setNeighbors(HashSet<BgpPeerConfig> neighbors) {
      this.neighbors = neighbors;
      return this;
    }

    public Builder setNetworks(HashMap<RoutePolicy, HashSet<Prefix>> networks) {
      this.networks = networks;
      return this;
    }

    public Builder setPreference(int[] preference) {
      this.preference = preference;
      return this;
    }

    public Builder setDefaultLocalPreference(int defaultLocalPreference) {
      this.defaultLocalPreference = defaultLocalPreference;
      return this;
    }

    public Builder setMaxLoadBalancingIbgp(int maxLoadBalancingIbgp) {
      this.maxLoadBalancingIbgp = maxLoadBalancingIbgp;
      return this;
    }

    public Builder setMaxLoadBalancingEbgp(int maxLoadBalancingEbgp) {
      this.maxLoadBalancingEbgp = maxLoadBalancingEbgp;
      return this;
    }

    public Builder setImports(HashSet<RedistributionConfig> imports) {
      this.imports = imports;
      return this;
    }

    public Builder setAggregations(Vector<RouteAggregation> aggregations) {
      this.aggregations = aggregations;
      return this;
    }
  }
}
