package inputparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.VirtualRouter;
import controlplane.process.RedistributionConfig;
import controlplane.route.RoutingProtocol;
import datamodel.routepolicy.RoutePolicy;

import java.util.HashSet;

import static util.JsonUtil.*;

public class RedistributionParser {
    public static HashSet<RedistributionConfig> parseImports(VirtualRouter vrf, JsonArray jsonImports) {
        HashSet<RedistributionConfig> imports = new HashSet<>();
        if (jsonImports != null) {
            for (int i = 0; i < jsonImports.size(); i++) {
                JsonObject jsonImport = jsonImports.get(i).getAsJsonObject();
                imports.add(parseImport(vrf, jsonImport));
            }
        }
        return imports;
    }

    public static RedistributionConfig parseImport(VirtualRouter vrf, JsonObject jsonImport) {
        RedistributionConfig.Builder builder = RedistributionConfig.builder();

        String srcVrfName = getAsStringOrDefault(jsonImport, "vrf", vrf.getVrfName());
        VirtualRouter srcVrf = vrf.getRouter().getVirtualRouters().get(srcVrfName);
        builder.setVrf(srcVrf);

        RoutingProtocol srcProtocol = RoutingProtocol.valueOf(jsonImport.get("protocol").getAsString().toUpperCase());
        builder.setProtocol(srcProtocol);

        Integer srcProcessId = getAsObjectDefaultNull(jsonImport, "processId", JsonElement::getAsInt);
        builder.setProcessId(srcProcessId);

        Long med = getAsObjectDefaultNull(jsonImport, "med", JsonElement::getAsLong);
        builder.setMed(med);

        Integer limit = getAsObjectDefaultNull(jsonImport, "limit", JsonElement::getAsInt);
        builder.setLimit(limit);

        Boolean permitIbgp = getAsObjectDefaultNull(jsonImport, "permitIbgp", JsonElement::getAsBoolean);
        builder.setPermitIbgp(permitIbgp);

        Long cost = getAsObjectDefaultNull(jsonImport, "cost", JsonElement::getAsLong);
        builder.setCost(cost);

        Integer type = getAsObjectDefaultNull(jsonImport, "type", JsonElement::getAsInt);
        builder.setType(type);

        Long tag = getAsObjectDefaultNull(jsonImport, "tag", JsonElement::getAsLong);
        builder.setTag(tag);

        // Make sure the routePolicy is not null, to avoid not-null-checks when redistributing routes into bgp/ospf
        String rpName = getAsStringDefaultNull(jsonImport, "routePolicy");
        RoutePolicy routePolicy = rpName == null ? RoutePolicy.PERMIT_ALL : vrf.getRouter().getRoutePolicy(rpName);
        builder.setRoutePolicy(routePolicy);

        return builder.build();
    }
}
