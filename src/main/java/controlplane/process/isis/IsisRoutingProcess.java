package controlplane.process.isis;

import com.google.common.collect.ImmutableSet;
import controlplane.network.Interface;
import controlplane.network.VirtualRouter;
import controlplane.rib.Rib;
import controlplane.route.IsisRoute;
import controlplane.route.Route;
import controlplane.route.RoutingProtocol;
import controlplane.route.builder.IsisRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.path.Path;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.RoutePolicy;
import main.Configuration;
import main.Controller;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisProcess;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class IsisRoutingProcess {
  VirtualRouter virtualRouter;

  IsisProcess process;

  Rib<IsisRoute> rib;
  Rib<IsisRoute> l1Rib;
  Rib<IsisRoute> l2Rib;

  RouteSet<IsisRoute> l1InQueue;
  RouteSet<IsisRoute> l2InQueue;
  RouteSet<IsisRoute> l1OutQueue;
  RouteSet<IsisRoute> l2OutQueue;

  public IsisRoutingProcess(VirtualRouter virtualRouter, IsisProcess process) {
    this.virtualRouter = virtualRouter;
    this.process = process;
    this.rib = new Rib<>();
    this.l1Rib = new Rib<>();
    this.l2Rib = new Rib<>();
    this.l1InQueue = new RouteSet<>();
    this.l2InQueue = new RouteSet<>();
    this.l1OutQueue = new RouteSet<>();
    this.l2OutQueue = new RouteSet<>();
  }

  public void begin() {
    /*
     * init L1 and L2 routes from connected routes
     */
    int l1Admin =
        Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(
            controlplane.route.RoutingProtocol.ISIS_L1);
    int l2Admin =
        Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(
            controlplane.route.RoutingProtocol.ISIS_L2);
    IsisLevelSettings l1Settings = process.getLevel1();
    IsisLevelSettings l2Settings = process.getLevel2();
    IsisRouteBuilder ifaceRouteBuilder =
        new IsisRouteBuilder()
            .setArea(process.getNetAddress().getAreaIdString())
            .setSystemId(process.getNetAddress().getSystemIdString());
    virtualRouter.getInterfaces().values().stream()
        .filter(Interface::isActive)
        .forEach(
            intf ->
                generateAllIsisInterfaceRoutes(
                    l1Admin, l2Admin, l1Settings, l2Settings, ifaceRouteBuilder, intf));

    // export default route for L1 neighbors on L1L2 routers that are not overloaded
    if (l1Settings != null && l2Settings != null && !process.getOverload()) {
      IsisRoute defaultRoute =
          new IsisRouteBuilder()
              .setAdmin(l1Admin)
              .setArea(process.getNetAddress().getAreaIdString())
              .setAttach(true)
              .setLevel(IsisLevel.LEVEL_1)
              .setMetric(0L)
              .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.ZERO))
              .setNextHopIp(Ip.AUTO)
              .setProtocol(RoutingProtocol.ISIS_L1)
              .setSystemId(process.getNetAddress().getSystemIdString())
              .build();
      l1OutQueue.add(defaultRoute);
    }

    // redistribute
    addRedistributedRoutesToDeltas();
  }

  /**
   * If IS-IS process redistributes routes, run the given routes through the IS-IS export policy and
   * merge any redistributable routes into the IS-IS level RIBs. Any changes will be recorded in the
   * given RIB deltas.
   */
  private void addRedistributedRoutesToDeltas() {
    if (process.getExportPolicy() == null) {
      return;
    }
    // If process has level 1 enabled, routes should be added to IS-IS rib as level 1.
    // Otherwise, if it has level 2 enabled, routes should be added as level 2.
    IsisLevelSettings activeLevelSettings = process.getLevel1();
    boolean isLevel1 = activeLevelSettings != null;
    if (!isLevel1) {
      activeLevelSettings = process.getLevel2();
    }
    if (activeLevelSettings == null) {
      // Neither level enabled
      return;
    }
    virtualRouter.getGlobalRib().getAllRoutes().stream()
        .filter(route -> !(route instanceof IsisRoute))
        .map(route -> exportNonIsisRouteToIsis(route, isLevel1))
        .filter(Objects::nonNull)
        .flatMap(routeSet -> routeSet.getAllRoutes().stream())
        .forEach(
            isisRoute -> {
              if (isisRoute.getLevel() == IsisLevel.LEVEL_1) {
                l1OutQueue.add(isisRoute);
              } else if (isisRoute.getLevel() == IsisLevel.LEVEL_2) {
                l2OutQueue.add(isisRoute);
              }
            });
  }

  /**
   * Generate IS-IS L1/L2 routes from a given interface and merge them into appropriate L1/L2 RIBs.
   */
  private void generateAllIsisInterfaceRoutes(
      int l1Admin,
      int l2Admin,
      @Nullable IsisLevelSettings l1Settings,
      @Nullable IsisLevelSettings l2Settings,
      IsisRouteBuilder routeBuilder,
      Interface iface) {
    IsisInterfaceSettings ifaceSettings = iface.getIsisIntfSettings();
    if (ifaceSettings == null) {
      return;
    }
    IsisInterfaceLevelSettings ifaceL1Settings = ifaceSettings.getLevel1();
    IsisInterfaceLevelSettings ifaceL2Settings = ifaceSettings.getLevel2();
    if (ifaceL1Settings != null && l1Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l1Admin, routeBuilder, iface, IsisLevel.LEVEL_1)
          .forEach(r -> l1OutQueue.add(r));
    }
    if (ifaceL2Settings != null && l2Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l2Admin, routeBuilder, iface, IsisLevel.LEVEL_2)
          .forEach(r -> l2OutQueue.add(r));
    }
  }

  /**
   * Generate IS-IS from a given interface for a given level (with a given metric/admin cost) and
   * merge them into the appropriate RIB.
   */
  private static Set<IsisRoute> generateIsisInterfaceRoutesPerLevel(
      int adminCost, IsisRouteBuilder routeBuilder, Interface iface, IsisLevel level) {
    IsisInterfaceLevelSettings ifaceLevelSettings =
        level == IsisLevel.LEVEL_1
            ? iface.getIsisIntfSettings().getLevel1()
            : iface.getIsisIntfSettings().getLevel2();
    RoutingProtocol isisProtocol =
        level == IsisLevel.LEVEL_1 ? RoutingProtocol.ISIS_L1 : RoutingProtocol.ISIS_L2;
    long metric =
        Objects.requireNonNull(ifaceLevelSettings).getMode() == IsisInterfaceMode.PASSIVE
            ? 0L
            : firstNonNull(
                ifaceLevelSettings.getCost(), org.batfish.datamodel.IsisRoute.DEFAULT_METRIC);
    routeBuilder.setAdmin(adminCost).setLevel(level).setMetric(metric).setProtocol(isisProtocol);
    return iface.getAllIpAddresses().stream()
        .map(
            address ->
                routeBuilder
                    .setPrefixesBdd(
                        Controller.bddManager
                            .getBddPrefixWrapper()
                            .encodePrefix(address.toPrefix()))
                    .setNextHopIp(address.getIp())
                    .build())
        .collect(ImmutableSet.toImmutableSet());
  }

  public void routeIn(IsisRoute route) {
    (route.getLevel() == IsisLevel.LEVEL_1 ? l1InQueue : l2InQueue).add(route);
  }

  public boolean updateRib() {
    if (!l1InQueue.isEmpty()) {
      l1Rib.sortInsert(l1InQueue.getAllRoutes());
      List<IsisRoute> l1Delta = l1Rib.getDelta();
      l1Delta.forEach(l1Route -> l1OutQueue.add(l1Route));
      l1Rib.clean();
      l1InQueue.clear();
    }
    if (!l2InQueue.isEmpty()) {
      l2Rib.sortInsert(l2InQueue.getAllRoutes());
      List<IsisRoute> l2Delta = l2Rib.getDelta();
      l2Delta.forEach(l2Route -> l2OutQueue.add(l2Route));
      l2Rib.clean();
      l2InQueue.clear();
    }
    return l1OutQueue.isEmpty() && l2OutQueue.isEmpty();
  }

  public void routeOut(IsisTopology isisTopology) {
    List<IsisRoute> l1Delta = l1OutQueue.getAllRoutes(), l2Delta = l2OutQueue.getAllRoutes();
    l1OutQueue.clear();
    l2OutQueue.clear();
    if (process.getOverload()) {
      l1Delta =
          l1Delta.stream()
              .map(l1Route -> l1Route.toBuilder().setOverload(true).build())
              .collect(Collectors.toList());
      l2Delta =
          l2Delta.stream()
              .map(l2Route -> l2Route.toBuilder().setOverload(true).build())
              .collect(Collectors.toList());
    }
    boolean upgradeL1Routes =
        process.getLevel1() != null && process.getLevel2() != null && !process.getOverload();

    List<IsisEdge> edges = isisTopology.inEdges(virtualRouter);
    for (IsisEdge edge : edges) {
      Interface intf = isisTopology.getInterface(edge.getNode2());
      IsisInterfaceLevelSettings level1Settings = intf.getIsisIntfSettings().getLevel1();
      IsisInterfaceLevelSettings level2Settings = intf.getIsisIntfSettings().getLevel2();
      IsisLevel activeLevels = null;
      if (level1Settings != null && level1Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.LEVEL_1;
      }
      if (level2Settings != null && level2Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.union(activeLevels, IsisLevel.LEVEL_2);
      }
      if (activeLevels == null) {
        continue;
      }

      IsisRoutingProcess remoteProcess =
          isisTopology.getInterface(edge.getNode1()).getVrf().getIsisProcess();
      IsisLevel circuitType = edge.getCircuitType();
      if (circuitType.includes(IsisLevel.LEVEL_1) && activeLevels.includes(IsisLevel.LEVEL_1)) {
        l1Delta.stream()
            .map(l1Route -> propagate(l1Route, edge, isisTopology))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(remoteProcess::routeIn);
      }
      if (circuitType.includes(IsisLevel.LEVEL_2) && activeLevels.includes(IsisLevel.LEVEL_2)) {
        l2Delta.stream()
            .map(l2Route -> propagate(l2Route, edge, isisTopology))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(remoteProcess::routeIn);
        if (upgradeL1Routes) {
          l1Delta.forEach(
              l1Route -> {
                RoutingProtocol l1Protocol = l1Route.getRoutingProtocol();
                RoutingProtocol upgradedProtocol;
                if (l1Protocol == RoutingProtocol.ISIS_L1) {
                  upgradedProtocol = RoutingProtocol.ISIS_L2;
                } else if (l1Protocol == RoutingProtocol.ISIS_EL1) {
                  upgradedProtocol = RoutingProtocol.ISIS_EL2;
                } else {
                  throw new IllegalStateException(
                      String.format("Unrecognized ISIS level 1 protocol: %s", l1Protocol));
                }
                int upgradeAdmin =
                    Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(upgradedProtocol);
                // upgrade route
                Optional<IsisRoute> optional =
                    convertRouteLevel1ToLevel2(l1Route, upgradedProtocol, upgradeAdmin);
                // propagate route
                if (optional.isPresent()) {
                  optional = propagate(optional.get(), edge, isisTopology);
                }
                // insert incoming queue
                if (optional.isPresent()) {
                  IsisRoute upgradedRoute = optional.get();
                  remoteProcess.routeIn(upgradedRoute);
                }
              });
        }
      }
    }
  }

  public void mergeLevelRoutes() {
    rib.sortInstall(l1Rib);
    if (!(process.getLevel1() != null && process.getLevel2() == null)) {
      rib.sortInstall(l2Rib);
    }
  }

  public Rib<IsisRoute> getRib() {
    return rib;
  }

  public static Optional<IsisRoute> propagate(
      IsisRoute route, IsisEdge edge, IsisTopology topology) {
    // avoid propagation loop
    if (route.getPath().contains(topology.getInterface(edge.getNode1()).getVrf())) {
      return Optional.empty();
    }

    Ip nextHopIp = topology.getInterface(edge.getNode1()).getIp();
    Interface intf = topology.getInterface(edge.getNode2());
    IsisLevel routeLevel = route.getLevel();
    IsisInterfaceLevelSettings isisLevelSettings =
        routeLevel == IsisLevel.LEVEL_1
            ? intf.getIsisIntfSettings().getLevel1()
            : intf.getIsisIntfSettings().getLevel2();

    // Do not propagate route if ISIS interface is not active at this level
    if ((Objects.requireNonNull(isisLevelSettings).getMode()) != IsisInterfaceMode.ACTIVE) {
      return Optional.empty();
    }

    int adminCost =
        Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(route.getRoutingProtocol());
    long incrementalMetric = firstNonNull(isisLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
    return Optional.of(
        route.toBuilder()
            .setAdmin(adminCost)
            .setLevel(routeLevel)
            .setMetric(incrementalMetric + route.getMetric())
            .setNextHopIp(nextHopIp)
            .setPath(new Path(route.getPath(), intf.getVrf()))
            .build());
  }

  public static Optional<IsisRoute> convertRouteLevel1ToLevel2(
      IsisRoute route, RoutingProtocol l2Protocol, int l2Admin) {
    if (route.getLevel() != IsisLevel.LEVEL_1 || route.getAttach() || route.getDown()) {
      return Optional.empty();
    }
    return Optional.of(
        route.toBuilder()
            .setAdmin(l2Admin)
            .setLevel(IsisLevel.LEVEL_2)
            .setProtocol(l2Protocol)
            .build());
  }

  /**
   * Given a {@link Route}, run it through IS-IS outbound transformations and export routing policy.
   *
   * @return A {@link RouteSet} of transformed {@link IsisRoute}s that pass the IS-IS export policy.
   */
  public RouteSet<IsisRoute> exportNonIsisRouteToIsis(Route route, boolean isLevel1) {
    RoutePolicy exportPolicy =
        Optional.ofNullable(process.getExportPolicy())
            .map(policyName -> virtualRouter.getRouter().getRoutePolicy(policyName))
            .orElse(null);
    if (exportPolicy == null) {
      // Export policy is undefined or not configured
      return null;
    }

    // Process transformed outgoing route through the export policy
    IsisRoute transformedOutgoingRoute = convertNonIsisRouteToIsisRoute(route, isLevel1);
    RouteFilterResult<IsisRoute> result =
        exportPolicy.filter(
            transformedOutgoingRoute,
            new RouteFilterEnvironment.Builder<IsisRoute>()
                .setRouter(virtualRouter.getRouter())
                .setIn(false)
                .setLocalConfig(null)
                .build());

    RoutingProtocol protocol = isLevel1 ? RoutingProtocol.ISIS_EL1 : RoutingProtocol.ISIS_EL2;
    RouteSet<IsisRoute> routes = new RouteSet<>();
    result
        .getPermitted()
        .getAllRoutes()
        .forEach(r -> routes.add(r.toBuilder().setProtocol(protocol).build()));
    return routes;
  }

  /**
   * Convert a non-ISIS route to an {@link IsisRoute}.
   *
   * <p>Intended for converting main RIB routes into their IS-IS equivalents before passing {@code
   * route} to the export policy.
   *
   * <p>Sets network, admin, metric, area, tag, level, overload, system ID. Does not set next hop
   * Ip, attach, or down.
   *
   * <p>Note that we do not set the protocol here since the export policy may match the original
   * protocol.
   */
  public IsisRoute convertNonIsisRouteToIsisRoute(Route route, boolean isLevel1) {
    assert !(route instanceof IsisRoute);
    return new IsisRouteBuilder()
        .setPrefixesBdd(route.getPrefixesBdd())
        .setAdmin(route.getAdmin())
        .setMetric(route.getMetric())
        .setArea(process.getNetAddress().getAreaIdString())
        .setTag(route.getTag())
        .setProtocol(route.getRoutingProtocol())
        .setLevel(isLevel1 ? IsisLevel.LEVEL_1 : IsisLevel.LEVEL_2)
        .setOverload(process.getOverload())
        .setSystemId(process.getNetAddress().getSystemIdString())
        .build();
  }
}
