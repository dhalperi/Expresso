package inputparser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.VirtualRouter;
import controlplane.process.RedistributionConfig;
import controlplane.process.ospf.*;
import datamodel.ipv4.Ip;

import java.util.*;

import static util.JsonUtil.getAsStringDefaultNull;

public class OspfParser {
    public static SortedMap<Integer, OspfRoutingProcess> parseOspfProcesses(VirtualRouter virtualRouter, JsonObject jsonOspfProcesses) {
        SortedMap<Integer, OspfRoutingProcess> ospfProcesses = new TreeMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonOspfProcesses.entrySet()) {
            OspfRoutingProcess ospfRoutingProcess = parseOspfRoutingProcess(virtualRouter, entry.getValue().getAsJsonObject());
            ospfProcesses.put(ospfRoutingProcess.getProcessId(), ospfRoutingProcess);
        }

        return ospfProcesses;
    }

    public static OspfRoutingProcess parseOspfRoutingProcess(VirtualRouter virtualRouter, JsonObject jsonOspfProcess) {
        // parse process id
        int processId = Integer.parseInt(jsonOspfProcess.get("processId").getAsString());

        // parse router id
        Ip routerId = Ip.parse(getAsStringDefaultNull(jsonOspfProcess, "routerId"));

        // parse reference bandwidth
        double referenceBandwidth = jsonOspfProcess.get("referenceBandwidth").getAsDouble();

        // parse ospf areas
        SortedMap<Long, OspfArea> ospfAreas = new TreeMap<>();
        jsonOspfProcess
                .getAsJsonObject("areas")
                .entrySet()
                .forEach(e -> {
                    OspfArea area = parseOspfArea(virtualRouter, referenceBandwidth, e.getValue().getAsJsonObject());
                    ospfAreas.put(area.getName(), area);
                });

        // todo parse export policy
        HashSet<RedistributionConfig> imports = RedistributionParser.parseImports(virtualRouter, jsonOspfProcess.getAsJsonArray("imports"));

        return OspfRoutingProcess.builder()
                .setVirtualRouter(virtualRouter)
                .setOspfAreas(ospfAreas)
                .setProcessId(processId)
                .setReferenceBandwidth(referenceBandwidth)
                .setRouterId(routerId)
                .setImports(imports)
                .build();
    }

    public static OspfArea parseOspfArea(VirtualRouter virtualRouter, double referenceBandwidth, JsonObject jsonOspfArea) {
        // parse name
        long name = jsonOspfArea.get("name").getAsLong();

        // parse inject default route
        boolean injectDefaultRoute = jsonOspfArea.get("injectDefaultRoute").getAsBoolean();

        // parse interfaces
        List<Interface> interfaces = new ArrayList<>();
        jsonOspfArea.getAsJsonArray("interfaces").forEach(e -> {
            InterfaceName intfName = new InterfaceName(virtualRouter.getRouter().getRouterName(), e.getAsString());
            Interface intf = virtualRouter.getRouter().getInterfaces().get(intfName);
            // set ospf cost of interface
            if (intf.getOspfIntfSetting().getOspfCost() == -1) {
                intf.getOspfIntfSetting().setOspfCost(Integer.max((int) (referenceBandwidth / intf.getBandwidth()), 1));
            }
            interfaces.add(intf);
        });

        // parse metric of default route
        int metricOfDefaultRoute = jsonOspfArea.get("metricOfDefaultRoute").getAsInt();

        return OspfArea.builder()
                .setName(name)
                .setInjectDefaultRoute(injectDefaultRoute)
                .setInterfaces(interfaces)
                .setMetricOfDefaultRoute(metricOfDefaultRoute)
                .build();
    }
}
