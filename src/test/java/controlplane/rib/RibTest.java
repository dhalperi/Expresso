package controlplane.rib;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.aspath.AsPath;
import datamodel.ipv4.Prefix;
import main.Controller;
import org.junit.Test;

public class RibTest {
    @Test
    public void testRib() {
        Controller.pushBDDManager(new BddManager());

        int bdd1 = Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("192.168.0.0/24"));
        int bdd2 = Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("100.0.0.0/24"));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute route1 = builder.setPrefixesBdd(bdd1).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute route11 = builder.build();
        BgpRoute route2 = builder.setPrefixesBdd(bdd1).setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).build();
        BgpRoute route3 = builder.setPrefixesBdd(bdd1).setAsPath(AsPath.ofSingletonAsSets(2L)).build();
        BgpRoute route4 = builder.setPrefixesBdd(bdd2).setAsPath(AsPath.ofSingletonAsSets(1L)).build();

        Rib<BgpRoute> rib = new Rib<>();
        rib.simpleInsert(route1);
        rib.simpleInsert(route11);
        rib.simpleInsert(route2);
        rib.simpleInsert(route3);
        rib.simpleInsert(route4);

        System.out.println();
    }

}