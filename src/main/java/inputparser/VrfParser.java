package inputparser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.isis.IsisRoutingProcess;
import controlplane.process.ospf.OspfRoutingProcess;
import datamodel.community.Community;
import datamodel.mpls.RouteDistinguisher;

import java.util.*;

import static util.JsonUtil.getAsStringDefaultNull;

public class VrfParser {
  public static SortedMap<String, VirtualRouter> parseVrfs(Router router, JsonObject jsonVrfs) {
    SortedMap<String, VirtualRouter> vrfs = new TreeMap<>();
    jsonVrfs
        .entrySet()
        .forEach(
            entry -> {
              String vrfName = entry.getKey();
              VirtualRouter virtualRouter = new VirtualRouter(router, vrfName);
              JsonObject jsonVrf = entry.getValue().getAsJsonObject();
              parseVrf(virtualRouter, jsonVrf);
              vrfs.put(vrfName, virtualRouter);
            });
    return vrfs;
  }

  public static void parseVrf(VirtualRouter virtualRouter, JsonObject jsonVrf) {
    // parse bgp
    BgpRoutingProcess bgpProcess = null;
    if (!jsonVrf.get("bgpProcess").isJsonNull()) {
      bgpProcess = BgpParser.parseBgpProcess(virtualRouter, jsonVrf.getAsJsonObject("bgpProcess"));
    }
    virtualRouter.setBgpProcess(bgpProcess);

    // parse ospf
    SortedMap<Integer, OspfRoutingProcess> ospfProcesses =
        OspfParser.parseOspfProcesses(virtualRouter, jsonVrf.getAsJsonObject("ospfProcesses"));
    virtualRouter.setOspfProcesses(ospfProcesses);

    IsisRoutingProcess isisProcess =
        IsisParser.parseIsisProcess(virtualRouter, jsonVrf.getAsJsonObject("isisProcess"));
    virtualRouter.setIsisProcess(isisProcess);

    // parse route distinguisher
    if (!jsonVrf.get("RD").isJsonNull()) {
      RouteDistinguisher RD = RouteDistinguisher.parse(getAsStringDefaultNull(jsonVrf, "RD"));
      virtualRouter.setRD(RD);
    }

    // parser route targets
    Set<Community> iRTs = new HashSet<>();
    Set<Community> eRTs = new HashSet<>();
    jsonVrf
        .getAsJsonObject("vpntarget")
        .entrySet()
        .forEach(
            entry -> {
              Community RT = CommunityParser.parseCommunity(entry.getKey());
              boolean isImport =
                  entry.getValue().getAsJsonObject().get("importExtcommunity").getAsBoolean();
              boolean isExport =
                  entry.getValue().getAsJsonObject().get("exportExtcommunity").getAsBoolean();
              if (isImport) iRTs.add(RT);
              if (isExport) eRTs.add(RT);
            });
    virtualRouter.setIRTs(iRTs);
    virtualRouter.setERTs(eRTs);

    // parse static routes
    StaticRouteParser.parseStaticRoute(virtualRouter, jsonVrf.getAsJsonArray("staticRoutes"));

    // parse interfaces
    for (JsonElement e : jsonVrf.getAsJsonArray("interfaces")) {
      InterfaceName interfaceName =
          new InterfaceName(virtualRouter.getRouter().getRouterName(), e.getAsString());
      Interface intf = virtualRouter.getRouter().getInterfaces().get(interfaceName);
      intf.setVrf(virtualRouter);
      virtualRouter.getInterfaces().put(interfaceName, intf);
    }
  }
}
