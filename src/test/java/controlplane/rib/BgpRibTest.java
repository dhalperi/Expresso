package controlplane.rib;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.community.CommunityListFactory;
import datamodel.community.StandardCommunity;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import main.Controller;
import org.junit.Test;

import java.util.Collections;

public class BgpRibTest {

  @Test
  public void testInsert() {
    Controller.pushBDDManager(new BddManager());

    String prefixStr = "5.244.208.0/20";
    Prefix prefix = Prefix.of(prefixStr);
    int prefixBdd = Controller.bddManager.getBddPrefixWrapper().encodePrefix(prefix);

    BgpRoute bgpRoute1 =
        new BgpRouteBuilder()
            .setPrefixesBdd(prefixBdd)
            .setNextHopIp(Ip.of("176.28.233.20"))
            .setType(BgpRoute.BgpRouteType.FROM_IBGP_NEIGHBOR)
            .setCommunityList(CommunityListFactory.of(StandardCommunity.parse("65311:12011")))
            .build();
    BgpRoute bgpRoute2 =
        new BgpRouteBuilder()
            .setPrefixesBdd(prefixBdd)
            .setNextHopIp(Ip.of("176.28.233.22"))
            .setType(BgpRoute.BgpRouteType.FROM_IBGP_NEIGHBOR)
            .build();
    BgpRib bgpRib = new BgpRib(false, true, false);
    bgpRib.simpleInsert(bgpRoute1);
    bgpRib.simpleInsert(bgpRoute2);
    bgpRib.getAllRoutes().forEach(System.out::println);
  }
}
