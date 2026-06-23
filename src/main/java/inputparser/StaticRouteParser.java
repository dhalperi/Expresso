package inputparser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.VirtualRouter;
import controlplane.route.builder.StaticRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import main.Controller;

import java.util.Objects;

import static util.JsonUtil.getAsStringDefaultNull;

public class StaticRouteParser {
    public static void parseStaticRoute(VirtualRouter virtualRouter, JsonArray jsonStaticRoutes) {
        Multimap<StaticRouteId, Prefix> multimap = HashMultimap.create();
        if (jsonStaticRoutes != null) {
            for (int i = 0; i < jsonStaticRoutes.size(); i++) {
                JsonObject jsonStaticRoute = jsonStaticRoutes.get(i).getAsJsonObject();

                // prefix
                Prefix prefix = Prefix.of(jsonStaticRoute.get("network").getAsString());

                // next hop ip
                Ip nextHopIp = Ip.parse(getAsStringDefaultNull(jsonStaticRoute, "nextHopIp"));

                // next hop interface
                Interface nextHopInterface = null;
                String tmp = jsonStaticRoute.get("nextHopInterface").getAsString();
                if (!tmp.equals("dynamic")) {
                    InterfaceName interfaceName = new InterfaceName(virtualRouter.getRouter().getRouterName(), jsonStaticRoute.get("nextHopInterface").getAsString());
                    nextHopInterface = virtualRouter.getRouter().getInterfaces().get(interfaceName);
                }

                // admin
                int admin = jsonStaticRoute.get("administrativeCost").getAsInt();

                // metric
                long metric = jsonStaticRoute.get("metric").getAsLong();

                // tag
                long tag = jsonStaticRoute.get("tag").getAsLong();

                StaticRouteId id = new StaticRouteId(nextHopIp, nextHopInterface, admin, metric, tag);
                multimap.put(id, prefix);
            }
        }
        multimap.asMap().forEach((id, prefixes) ->
                virtualRouter.insertStaticRoute(new StaticRouteBuilder()
                        .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefixes(prefixes))
                        .setNextHopIp(id.nextHopIp)
                        .setNextHopInterface(id.nextHopInterface)
                        .setPreference(id.admin)
//                        .setAdmin(id.admin)
                        .setTag(id.tag)
                        .setMetric(id.metric)
                        .build()));
    }

    private static class StaticRouteId {
        Ip nextHopIp;
        Interface nextHopInterface;
        int admin;
        long metric;
        long tag;

        public StaticRouteId(Ip nextHopIp, Interface nextHopInterface, int admin, long metric, long tag) {
            this.nextHopIp = nextHopIp;
            this.nextHopInterface = nextHopInterface;
            this.admin = admin;
            this.metric = metric;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StaticRouteId that = (StaticRouteId) o;
            return admin == that.admin
                    && metric == that.metric
                    && tag == that.tag
                    && Objects.equals(nextHopIp, that.nextHopIp)
                    && Objects.equals(nextHopInterface, that.nextHopInterface);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nextHopIp, nextHopInterface, admin, metric, tag);
        }
    }
}
