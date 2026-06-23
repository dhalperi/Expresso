package controlplane.process.bgp.routeaggregation;

import controlplane.route.BgpRoute;
import datamodel.ipv4.Prefix;
import datamodel.route.RouteSet;
import main.Controller;

import java.util.Collection;

public class RouteAggregationResult {
    Prefix prefix;
    int prefixBdd;
    RouteSet<BgpRoute> specifics;
    RouteSet<BgpRoute> origins;
    RouteSet<BgpRoute> suppressed;
    RouteSet<BgpRoute> notSuppressed;
    RouteSet<BgpRoute> others;
    RouteSet<BgpRoute> aggrs;

    private RouteAggregationResult() {
        this.suppressed = new RouteSet<>();
        this.others = new RouteSet<>();
        this.aggrs = new RouteSet<>();
    }

    public RouteAggregationResult(Prefix prefix) {
        this.prefix = prefix;
        this.prefixBdd = Controller.bddManager.getBddPrefixWrapper().encodePrefix(prefix);
        this.specifics = new RouteSet<>();
        this.origins = new RouteSet<>();
        this.suppressed = new RouteSet<>();
        this.notSuppressed = new RouteSet<>();
        this.others = new RouteSet<>();
        this.aggrs = new RouteSet<>();
    }

    public RouteSet<BgpRoute> getSuppressed() {
        return suppressed;
    }

    public RouteSet<BgpRoute> getOthers() {
        return others;
    }

    public RouteSet<BgpRoute> getAggrs() {
        return aggrs;
    }

    public static RouteAggregationResult combine(Collection<RouteAggregationResult> results) {
        RouteAggregationResult combined = new RouteAggregationResult();
        results.forEach(result -> {
            combined.suppressed.addAll(result.suppressed);
            combined.others.addAll(result.notSuppressed);
            combined.others.addAll(result.others);
            combined.aggrs.addAll(result.aggrs);
        });
        combined.others.removeAll(combined.suppressed);
        combined.suppressed.getAllRoutes().forEach(route -> route.setSuppressed(true));

        return combined;
    }

    @Override
    public String toString() {
        return String.join("\n",
                "RouteAggregationResult{",
                "suppressed=" + suppressed.toString(),
                "notSuppressed=" + notSuppressed.toString(),
                "others=" + others.toString(),
                "aggrs=" + aggrs.toString(),
                "}"
                );
    }
}
