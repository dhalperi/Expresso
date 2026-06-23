package inputparser.bfvi;

import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.bgp.routeaggregation.RouteAggregation;
import controlplane.process.isis.IsisRoutingProcess;
import controlplane.process.ospf.OspfRoutingProcess;
import datamodel.ipv4.Prefix;
import datamodel.mpls.RouteDistinguisher;
import datamodel.routepolicy.RoutePolicy;
import inputparser.IsisParser;
import inputparser.OspfParser;
import inputparser.StaticRouteParser;

import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import static util.JsonUtil.getAsObjectDefaultNull;
import static util.JsonUtil.getAsStringDefaultNull;

public class VrfParser {
  public static SortedMap<String, VirtualRouter> parseVrfs(
      Router router, JsonObject jsonVrfs, Multimap<String, InterfaceName> vrfInterfaces) {
    SortedMap<String, VirtualRouter> vrfs = new TreeMap<>();

    Vector<RouteAggregation> routeAggregations =
        router.getRoutePolicies().keySet().stream()
            .filter(routePolicy -> routePolicy.contains("AGGREGATE_ROUTE_POLICY"))
            .map(
                routePolicy -> {
                  String aggr = routePolicy.split(":")[1];
                  aggr = aggr.substring(0, aggr.length() - 1);
                  return new RouteAggregation(Prefix.of(aggr), false, false, null, null, null);
                })
            .collect(Collectors.toCollection(Vector::new));

    Map<String, RoutePolicy> routeImports =
        router.getRoutePolicies().entrySet().stream()
            .filter(e -> e.getKey().contains("REDISTRIBUTION"))
            .collect(
                Collectors.toMap(
                    e -> e.getKey().substring(0, e.getKey().length() - "-REDISTRIBUTION".length()),
                    Map.Entry::getValue));

    jsonVrfs
        .entrySet()
        .forEach(
            entry -> {
              String vrfName = entry.getKey();
              VirtualRouter virtualRouter = new VirtualRouter(router, vrfName);
              JsonObject jsonVrf = entry.getValue().getAsJsonObject();
              parseVrf(virtualRouter, vrfInterfaces, routeAggregations, routeImports, jsonVrf);
              vrfs.put(vrfName, virtualRouter);
            });
    return vrfs;
  }

  public static void parseVrf(
      VirtualRouter virtualRouter,
      Multimap<String, InterfaceName> vrfInterfaces,
      Vector<RouteAggregation> routeAggregations,
      Map<String, RoutePolicy> routeImports,
      JsonObject jsonVrf) {
    // parse bgp
    BgpRoutingProcess bgpProcess = null;
    if (!jsonVrf.get("bgpProcess").isJsonNull()) {
      bgpProcess =
          BgpParser.parseBgpProcess(
              virtualRouter,
              routeAggregations,
              routeImports,
              jsonVrf.getAsJsonObject("bgpProcess"));
    }
    virtualRouter.setBgpProcess(bgpProcess);

    // parse ospf
    SortedMap<Integer, OspfRoutingProcess> ospfProcesses =
        OspfParser.parseOspfProcesses(virtualRouter, jsonVrf.getAsJsonObject("ospfProcesses"));
    virtualRouter.setOspfProcesses(ospfProcesses);

    IsisRoutingProcess isisProcess =
        IsisParser.parseIsisProcess(
            virtualRouter,
            getAsObjectDefaultNull(jsonVrf, "isisProcess", JsonElement::getAsJsonObject));
    virtualRouter.setIsisProcess(isisProcess);

    // parse route distinguisher
    if (getAsObjectDefaultNull(jsonVrf, "RD", JsonElement::getAsJsonObject) != null) {
      RouteDistinguisher RD = RouteDistinguisher.parse(getAsStringDefaultNull(jsonVrf, "RD"));
      virtualRouter.setRD(RD);
    }

    // parser route targets
    virtualRouter.setIRTs(new HashSet<>());
    virtualRouter.setERTs(new HashSet<>());

    // parse static routes
    StaticRouteParser.parseStaticRoute(virtualRouter, jsonVrf.getAsJsonArray("staticRoutes"));

    // parse interfaces
    for (InterfaceName interfaceName : vrfInterfaces.get(jsonVrf.get("name").getAsString())) {
      Interface intf = virtualRouter.getRouter().getInterfaces().get(interfaceName);
      intf.setVrf(virtualRouter);
      virtualRouter.getInterfaces().put(interfaceName, intf);
    }
  }
}
