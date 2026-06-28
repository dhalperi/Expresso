package controlplane.network;

import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.isis.IsisRoutingProcess;
import controlplane.process.ospf.OspfRoutingProcess;
import controlplane.rib.RecursiveResolver;
import controlplane.rib.Rib;
import controlplane.route.ConnectedRoute;
import controlplane.route.LocalRoute;
import controlplane.route.Route;
import controlplane.route.StaticRoute;
import controlplane.route.builder.ConnectedRouteBuilder;
import controlplane.route.builder.LocalRouteBuilder;
import datamodel.community.Community;
import datamodel.ipv4.Ip;
import datamodel.ipv4.IpAddress;
import datamodel.ipv4.Prefix;
import datamodel.mpls.RouteDistinguisher;
import datamodel.path.PathHop;
import datamodel.route.RouteSet;
import dataplane.fib.Fib;
import main.Controller;

import java.util.*;
import java.util.stream.Collectors;

// In Batfish, they put all routing message processing work in VirtualRouter
public class VirtualRouter implements PathHop {
  Router router;
  String vrfName;

  SortedMap<InterfaceName, Interface> interfaces;
  Interface sink;

  SortedMap<Integer, OspfRoutingProcess> ospfProcesses;
  IsisRoutingProcess isisProcess;
  BgpRoutingProcess bgpProcess;

  RouteDistinguisher RD;
  Set<Community> iRTs;
  Set<Community> eRTs;

  Rib<ConnectedRoute> connectedRib;
  Rib<LocalRoute> localRib;
  RouteSet<StaticRoute> conditionalStaticRib;
  RouteSet<StaticRoute> unconditionalStaticRib;
  Rib<Route> globalRib;
  Fib fib;

  public VirtualRouter(Router router, String vrfName) {
    this.router = router;
    this.vrfName = vrfName;
    this.interfaces = new TreeMap<>();
    connectedRib = new Rib<>();
    localRib = new Rib<>();
    conditionalStaticRib = new RouteSet<>();
    unconditionalStaticRib = new RouteSet<>();
    globalRib = new Rib<>();
    fib = new Fib(this);
  }

  public Map<InterfaceName, Interface> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(SortedMap<InterfaceName, Interface> interfaces) {
    this.interfaces = interfaces;
  }

  public SortedMap<Integer, OspfRoutingProcess> getOspfProcesses() {
    return ospfProcesses;
  }

  public void setOspfProcesses(SortedMap<Integer, OspfRoutingProcess> ospfProcesses) {
    this.ospfProcesses = ospfProcesses;
  }

  public IsisRoutingProcess getIsisProcess() {
    return isisProcess;
  }

  public void setIsisProcess(IsisRoutingProcess isisProcess) {
    this.isisProcess = isisProcess;
  }

  public BgpRoutingProcess getBgpProcess() {
    return bgpProcess;
  }

  public void setBgpProcess(BgpRoutingProcess bgpProcess) {
    this.bgpProcess = bgpProcess;
  }

  public void setRD(RouteDistinguisher RD) {
    this.RD = RD;
  }

  public RouteDistinguisher getRD() {
    return RD;
  }

  public void setIRTs(Set<Community> iRTs) {
    this.iRTs = iRTs;
  }

  public Set<Community> getiRTs() {
    return iRTs;
  }

  public void setERTs(Set<Community> eRTs) {
    this.eRTs = eRTs;
  }

  public Set<Community> geteRTs() {
    return eRTs;
  }

  public Router getRouter() {
    return router;
  }

  public String getVrfName() {
    return vrfName;
  }

  public String getFullName() {
    return String.format("Vrf(%s, %s)", router.routerName, vrfName);
  }

  public Rib<ConnectedRoute> getConnectedRib() {
    return connectedRib;
  }

  public Rib<LocalRoute> getLocalRib() {
    return localRib;
  }

  public RouteSet<StaticRoute> getConditionalStaticRib() {
    return conditionalStaticRib;
  }

  public RouteSet<StaticRoute> getUnconditionalStaticRib() {
    return unconditionalStaticRib;
  }

  public Rib<Route> getGlobalRib() {
    return globalRib;
  }

  public Fib getFib() {
    return fib;
  }

  public void genConnectedRoute() {
    if (sink == null) {
      sink =
          Interface.builder()
              .setInterfaceName(
                  new InterfaceName(router.routerName, String.format("sink_%s", vrfName)))
              .setType(Interface.InterfaceType.LOOPBACK)
              .build();
      interfaces.put(sink.interfaceName, sink);
      router.interfaces.put(sink.interfaceName, sink);
    }

    // generate local routes
    Set<Prefix> localPrefixes =
        interfaces.values().stream()
            .filter(intf -> !intf.isBlackhole() && intf.isActive())
            .flatMap(intf -> intf.getAllIpAddresses().stream())
            //            .filter(addr -> addr.getNetworkBits() < 32)
            .map(IpAddress::toPrefix32)
            .collect(Collectors.toSet());
    LocalRoute localRoute =
        new LocalRouteBuilder()
            .setPrefixesBdd(
                Controller.bddManager.getBddPrefixWrapper().encodePrefixes(localPrefixes))
            .setNextHopIp(Ip.AUTO)
            .setNextHopInterface(sink)
            .build();
    localRib.simpleInsert(localRoute);

    for (Interface intf : interfaces.values()) {
      if (!intf.isBlackhole() && intf.isActive() && !intf.getAllIpAddresses().isEmpty()) {
        // generate connected routes
        Collection<Prefix> connectedPrefixes =
            intf.getAllIpAddresses().stream()
                //                        .filter(addr -> addr.getNetworkBits() < 32)
                .map(IpAddress::toPrefix)
                .collect(Collectors.toList());
        ConnectedRoute connectedRoute =
            new ConnectedRouteBuilder()
                .setPrefixesBdd(
                    Controller.bddManager.getBddPrefixWrapper().encodePrefixes(connectedPrefixes))
                .setNextHopIp(Ip.AUTO)
                .setNextHopInterface(intf)
                .build();
        connectedRib.simpleInsert(connectedRoute);
      }
    }
  }

  public void insertStaticRoute(StaticRoute route) {
    if (route.getNextHopInterface() != null) {
      // static routes with next-hop-interface, unconditional
      if (route.getNextHopIp() == null) route.setNextHopIp(Ip.AUTO);
      unconditionalStaticRib.add(route);
    } else if (route.getNextHopIp() == null) {
      // No next-hop IP and no next-hop interface: a discard/blackhole route (e.g. Juniper
      // "next-hop discard" / a null interface) or a next-vrf-only leak route. Expresso does not
      // forward these, so model them as unconditional blackhole routes.
      route.setNextHopIp(Ip.AUTO);
      route.setNextHopInterface(router.getBlackhole());
      unconditionalStaticRib.add(route);
    } else if (route.getNextHopIp().equals(Ip.ZERO)) {
      // static routes with next-hop-ip 0.0.0.0, discard matching packets, unconditional
      route.setNextHopIp(Ip.AUTO);
      route.setNextHopInterface(router.getBlackhole());
      unconditionalStaticRib.add(route);
    } else {
      // static routes with next-hop-ip, needs resolution
      conditionalStaticRib.add(route);
    }
  }

  public void installOriginalRoutesToGlobalRib() {
    globalRib.simpleInstall(connectedRib);
    globalRib.simpleInstall(localRib);
    activeConditionalStaticRoutes();
//    globalRib.sortInsert(unconditionalStaticRib.getAllRoutes());
  }

  public void installIgpRoutesToGlobalRib() {
    ospfProcesses.values().forEach(ospfProcess -> globalRib.sortInstall(ospfProcess.getRib()));
    if (isisProcess != null) globalRib.sortInstall(isisProcess.getRib());
  }

  public void activeConditionalStaticRoutes() {
    RouteSet<StaticRoute> staticRoutes = new RouteSet<>(unconditionalStaticRib.getAllRoutes());
    globalRib.sortInsert(staticRoutes.getAllRoutes());

    // recursively active the conditional static routes, since an activated static route may further
    // activate other static routes
    while (true) {
      Set<StaticRoute> activated =
          conditionalStaticRib.getAllRoutes().stream()
              .filter(route -> route.getNextHopInterface() == null)
              .filter(
                  route -> {
                    Set<RecursiveResolver.ResolutionResult> results =
                        RecursiveResolver.resolveRoute(route, globalRib);
                    if (!results.isEmpty()) {
                      route.setNextHopInterface(
                          results.iterator().next().getFinalNextHopInterface());
                    }
                    return !results.isEmpty();
                  })
              .collect(Collectors.toSet());
      if (activated.isEmpty()) {
        break;
      } else {
        staticRoutes.addAll(activated);
        globalRib.sortInsert(staticRoutes.getAllRoutes());
      }
    }
  }

  public void installBgpRoutesToGlobalRib() {
    if (bgpProcess != null) {
      globalRib.sortInstall(bgpProcess.getRib());
    }
  }

  public void computeFib() {
    globalRib.computeFib(fib);
  }

  @Override
  public String hopString() {
    return getFullName();
  }

  @Override
  public String toString() {
    return getFullName();
  }
}
