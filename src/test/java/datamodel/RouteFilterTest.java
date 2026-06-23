package datamodel;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.Mode;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import javafx.util.Pair;
import main.Controller;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RouteFilterTest {
  public void init() {
    Controller.bddManager = new BddManager();
  }

  @Test
  public void testPrefixRange() {
    init();
    PrefixRange prefixRange = PrefixRange.of(Prefix.of("128.0.0.0"), new Range<>(24, 32));
    BgpRoute bgpRoute = new BgpRouteBuilder().setPrefixesBdd(1).build();
    RouteFilterResult<BgpRoute> result =
        prefixRange.filter(
            bgpRoute,
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    int permitted = prefixRange.toBdd();
    int denied = Controller.bddManager.not(permitted);
    assertEquals(
        result.getPermitted().getAllRoutes().iterator().next().getPrefixesBdd(), permitted);
    assertEquals(result.getDenied().getAllRoutes().iterator().next().getPrefixesBdd(), denied);
  }

  @Test
  public void testPrefixList() {
    init();

    // build prefix list
    PrefixRange prefixRange1 = PrefixRange.of(Prefix.of("100.0.0.0"), new Range<>(24, 32));
    PrefixRange prefixRange2 = PrefixRange.of(Prefix.of("200.0.0.0"), new Range<>(30, 32));
    List<Pair<CanBeMatched, Mode>> prefixList = new ArrayList<>();
    prefixList.add(new Pair<>(prefixRange1, Mode.PERMIT));
    prefixList.add(new Pair<>(prefixRange2, Mode.PERMIT));
    FilterList filterList = new FilterList("prefixList", prefixList);

    // build bgp route
    BgpRoute bgpRoute = new BgpRouteBuilder().setPrefixesBdd(1).build();

    RouteFilterResult<BgpRoute> result =
        filterList.filter(
            bgpRoute,
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());

    int permitted = Controller.bddManager.or(prefixRange1.toBdd(), prefixRange2.toBdd());
    int denied = Controller.bddManager.not(permitted);

    assertEquals(
        result.getPermitted().getAllRoutes().iterator().next().getPrefixesBdd(), permitted);
    assertEquals(result.getDenied().getAllRoutes().iterator().next().getPrefixesBdd(), denied);
  }

  @Test
  public void testRouteFilter() {}
}
