package controlplane.process.ospf;

import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import controlplane.network.VirtualRouter;
import controlplane.process.Process;
import controlplane.process.RedistributionConfig;
import controlplane.rib.Rib;
import controlplane.route.OspfRoute;
import controlplane.route.Route;
import controlplane.route.RoutingProtocol;
import controlplane.route.builder.OspfRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.RoutePolicy;
import main.Controller;
import main.ExpressoLogger;

import java.util.*;
import java.util.stream.Collectors;

public class OspfRoutingProcess implements Process {
  VirtualRouter virtualRouter;

  int processId;
  double referenceBandwidth;
  Ip routerId;
  SortedMap<Long, OspfArea> ospfAreas;

  SortedMap<Long, RouteSet<OspfRoute>> inQueues;
  SortedMap<Long, RouteSet<OspfRoute>> outQueues;

  Rib<OspfRoute> rib;

  /** route redistribution */
  HashSet<RedistributionConfig> imports;

  private OspfRoutingProcess(
      VirtualRouter virtualRouter,
      int processId,
      double referenceBandwidth,
      Ip routerId,
      SortedMap<Long, OspfArea> ospfAreas,
      HashSet<RedistributionConfig> imports) {
    this.virtualRouter = virtualRouter;
    this.processId = processId;
    this.referenceBandwidth = referenceBandwidth;
    this.routerId = routerId;
    this.ospfAreas = ospfAreas;
    this.imports = imports;
    this.inQueues = new TreeMap<>();
    this.outQueues = new TreeMap<>();
    this.ospfAreas
        .keySet()
        .forEach(
            areaNum -> {
              inQueues.put(areaNum, new RouteSet<>());
              outQueues.put(areaNum, new RouteSet<>());
            });
    this.rib = new Rib<>();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Check whether an interface is active in this ospf process. An interface can only be active in
   * one OSPF process at a time
   */
  public boolean ownInterface(Interface intf) {
    return ospfAreas.values().stream().anyMatch(area -> area.interfaces.contains(intf));
  }

  public void begin() {
    for (OspfArea area : ospfAreas.values()) {
      Set<Interface> validIntfs =
          area.interfaces.stream()
              .filter(Interface::isActive)
              .filter(intf -> intf.getOspfIntfSetting().ospfEnabled)
              .filter(intf -> intf.getPrimaryIpAddress() != null)
              .collect(Collectors.toSet());
      SortedSet<Integer> intfCosts =
          validIntfs.stream()
              .map(intf -> intf.getOspfIntfSetting().ospfCost)
              .collect(Collectors.toCollection(TreeSet::new));
      for (int cost : intfCosts) {
        Set<Prefix> prefixes =
            validIntfs.stream()
                .filter(intf -> intf.getOspfIntfSetting().ospfCost == cost)
                .map(intf -> intf.getPrimaryIpAddress().toPrefix())
                .collect(Collectors.toSet());
        if (!prefixes.isEmpty()) {
          OspfRoute ospfRoute =
              new OspfRouteBuilder()
                  .setPrefixesBdd(
                      Controller.bddManager.getBddPrefixWrapper().encodePrefixes(prefixes))
                  .setMetric(cost)
                  .build();
          outQueues.get(area.name).add(ospfRoute);
        }
      }
    }
    redistribute();
  }

  // todo
  void redistribute() {
    if (imports != null) {
      RouteSet<OspfRoute> generatedRoutes = new RouteSet<>();
      for (RedistributionConfig im : imports) {
        if (im.getProtocol() == RoutingProtocol.CONNECTED) {
          virtualRouter
              .getConnectedRib()
              .getAllRoutes()
              .forEach(cr -> generatedRoutes.addAll(redistribute(cr, im.getRoutePolicy())));
        }
        if (im.getProtocol() == RoutingProtocol.STATIC) {
          virtualRouter.getGlobalRib().getAllRoutes().stream()
              .filter(route -> route.getRoutingProtocol() == RoutingProtocol.STATIC)
              .forEach(sr -> generatedRoutes.addAll(redistribute(sr, im.getRoutePolicy())));
        }
        if (im.getProtocol() == RoutingProtocol.OSPF) {
          virtualRouter
              .getOspfProcesses()
              .get(im.getProcessId())
              .getRib()
              .getAllRoutes()
              .forEach(r -> generatedRoutes.addAll(redistribute(r, im.getRoutePolicy())));
        }
        if (im.getProtocol() == RoutingProtocol.BGP) {
          ExpressoLogger.log(
              ExpressoLogger.LEVEL.ERROR, "Unsupported route redistribution from bgp to ospf");
        }
      }
      generatedRoutes
          .getAllRoutes()
          .forEach(
              generatedRoute -> {
                rib.simpleInsert(generatedRoute);
                outQueues.values().forEach(list -> list.add(generatedRoute));
              });
    }
  }

  RouteSet<OspfRoute> redistribute(Route route, RoutePolicy routingPolicy) {
    OspfRoute ospfRoute =
        new OspfRouteBuilder()
            .setPrefixesBdd(route.getPrefixesBdd())
            .setNextHopIp(route.getNextHopIp())
            .setNextHopInterface(route.getNextHopInterface())
            .setMetric(20)
            .build();
    RouteFilterResult<OspfRoute> result =
        routingPolicy.filter(
            ospfRoute,
            new RouteFilterEnvironment.Builder<OspfRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    return result.getPermitted();
  }

  public void routeIn(long area, OspfRoute route) {
    inQueues.get(area).add(route);
  }

  public boolean updateRib() {
    for (Map.Entry<Long, RouteSet<OspfRoute>> entry : inQueues.entrySet()) {
      long areaNum = entry.getKey();
      RouteSet<OspfRoute> inQueue = entry.getValue();
      if (!inQueue.isEmpty()) {
        // step 1. insert incoming routes into rib.
        rib.sortInsert(inQueue.getAllRoutes());

        // step 2. insert dirty routes into out queue.
        rib.getDelta().forEach(route -> outQueues.get(areaNum).add(route));

        // todo inter-area route propagation
        // step 3. make summary route and insert it into other areas' out queue.
        //                OspfRoute summary = new OspfRouteBuilder().build();
        //                ospfAreas
        //                        .keySet()
        //                        .stream()
        //                        .filter(num -> areaNum != num)
        //                        .forEach(num -> outQueues.get(num).add(summary));

        // step 4. clean rib.
        rib.clean();

        // step 5. clear it
        inQueue.clear();
      }
    }
    return outQueues.values().stream().allMatch(RouteSet::isEmpty);
  }

  public void routeOut(Map<OspfRoutingProcess, OspfEdge> edges) {
    outQueues.forEach(
        (areaNum, outQueue) -> {
          for (OspfRoute route : outQueue.getAllRoutes()) {
            edges.forEach(
                (p, e) -> {
                  if (ospfAreas.get(areaNum).interfaces.contains(e.src.intf)) {
                    // avoid propagation path loop
                    if (!route.getPath().contains(p.virtualRouter)) {
                      OspfExecutor.propagate(route, e);
                    }
                  }
                });
          }
          outQueue.clear();
        });
  }

  public VirtualRouter getVirtualRouter() {
    return virtualRouter;
  }

  public int getProcessId() {
    return processId;
  }

  @Override
  public Rib<OspfRoute> getRib() {
    return rib;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("vrf", virtualRouter)
        .add("routerId", routerId)
        .toString();
  }

  public static class Builder {
    VirtualRouter virtualRouter;
    int processId;
    double referenceBandwidth;
    Ip routerId;
    SortedMap<Long, OspfArea> ospfAreas;
    HashSet<RedistributionConfig> imports;

    private Builder() {}

    public OspfRoutingProcess build() {
      return new OspfRoutingProcess(
          virtualRouter, processId, referenceBandwidth, routerId, ospfAreas, imports);
    }

    public Builder setVirtualRouter(VirtualRouter virtualRouter) {
      this.virtualRouter = virtualRouter;
      return this;
    }

    public Builder setProcessId(int processId) {
      this.processId = processId;
      return this;
    }

    public Builder setReferenceBandwidth(double referenceBandwidth) {
      this.referenceBandwidth = referenceBandwidth;
      return this;
    }

    public Builder setRouterId(Ip routerId) {
      this.routerId = routerId;
      return this;
    }

    public Builder setOspfAreas(SortedMap<Long, OspfArea> ospfAreas) {
      this.ospfAreas = ospfAreas;
      return this;
    }

    public Builder setImports(HashSet<RedistributionConfig> imports) {
      this.imports = imports;
      return this;
    }
  }
}
