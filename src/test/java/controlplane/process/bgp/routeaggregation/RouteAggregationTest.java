package controlplane.process.bgp.routeaggregation;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.ipv4.Prefix;
import main.Controller;
import org.junit.Test;

import java.util.Collections;

public class RouteAggregationTest {

    @Test
    public void aggregate() {
        Controller.pushBDDManager(new BddManager());

        Prefix general = Prefix.of("128.0.0.0/24");
        Prefix specific1 = Prefix.of("128.0.0.0/25");
        Prefix specific2 = Prefix.of("192.0.0.0/25");

        RouteAggregation aggregation = new RouteAggregation(
                general, false, false, null, null, null);

        BgpRoute route = new BgpRouteBuilder()
                .setPrefixesBdd(Controller.bddManager.or(
                        Controller.bddManager.getBddPrefixWrapper().encodePrefix(specific1),
                        Controller.bddManager.getBddPrefixWrapper().encodePrefix(specific2)))
                .build();

        RouteAggregationResult result = aggregation.process(Collections.singleton(route));
        System.out.println(result);
    }
}