package datamodel.routepolicy;

import bdd.BddManager;
import com.google.common.collect.ImmutableSortedMap;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.Range;
import datamodel.community.StandardCommunity;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.Mode;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetCommunity;
import datamodel.routepolicy.match.MatchAll;
import datamodel.routepolicy.match.MatchFilterList;
import datamodel.routepolicy.match.MatchOne;
import javafx.util.Pair;
import junit.framework.TestCase;
import main.Controller;
import util.BddUtil;

import java.util.LinkedList;
import java.util.List;

public class RoutePolicyTest extends TestCase {

  public static Pair<RoutePolicy, BgpRoute> getRoutePolicy() {
    PrefixRange pr1 = PrefixRange.of(Prefix.of("10.1.4.0/24"), new Range<>(24, 24));
    PrefixRange pr2 = PrefixRange.of(Prefix.of("10.1.5.0/24"), new Range<>(24, 24));
    PrefixRange pr3 = PrefixRange.of(Prefix.of("10.1.6.0/24"), new Range<>(24, 24));
    PrefixRange pr4 = PrefixRange.of(Prefix.of("0.0.0.0/0"), new Range<>(0, 32));

    List<Pair<CanBeMatched, Mode>> l1 = new LinkedList<>();
    l1.add(new Pair<>(pr1, Mode.DENY));
    l1.add(new Pair<>(pr2, Mode.PERMIT));
    FilterList pl1 = new FilterList("list1", l1);
    List<Pair<CanBeMatched, Mode>> l2 = new LinkedList<>();
    l2.add(new Pair<>(pr1, Mode.DENY));
    l2.add(new Pair<>(pr4, Mode.PERMIT));
    FilterList pl2 = new FilterList("list2", l2);

    List<Action> a1 = new LinkedList<>();
    a1.add(SetCommunity.increase(StandardCommunity.parse("64512:100")));
    List<Action> a2 = new LinkedList<>();
    a2.add(SetCommunity.increase(StandardCommunity.parse("64512:200")));
    List<Action> a3 = new LinkedList<>();
    a3.add(SetCommunity.increase(StandardCommunity.parse("64512:300")));

    Node node1 =
        new Node(Mode.DENY, new MatchAll(new MatchOne(new MatchFilterList("list1", pl1))), a1);
    Node node2 =
        new Node(Mode.PERMIT, new MatchAll(new MatchOne(new MatchFilterList("list2", pl2))), a2);
    Node node3 = new Node(Mode.PERMIT, new MatchAll(), a3);

    ImmutableSortedMap.Builder<Integer, Node> nodes = ImmutableSortedMap.naturalOrder();
    nodes.put(100, node1).put(200, node2).put(300, node3);
    RoutePolicy routePolicy = new RoutePolicy("route-policy", nodes.build());

    assert pr1 != null;
    assert pr2 != null;
    assert pr3 != null;
    int[] tmp = new int[] {pr1.toBdd(), pr2.toBdd(), pr3.toBdd()};
    BgpRoute bgpRoute =
        new BgpRouteBuilder()
            .setPrefixesBdd(BddUtil.orInBatch(Controller.bddManager.getBDD(), tmp))
            .build();

    return new Pair<>(routePolicy, bgpRoute);
  }

  public void testFilter() {
    Controller.pushBDDManager(new BddManager());

    Pair<RoutePolicy, BgpRoute> pair = getRoutePolicy();
    RoutePolicy routePolicy = pair.getKey();
    BgpRoute bgpRoute = pair.getValue();

    RouteFilterResult<BgpRoute> result =
        routePolicy.filter(
            bgpRoute,
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    System.out.println("permitted:");
    for (BgpRoute permitted : result.getPermitted().getAllRoutes()) {
      System.out.println(permitted);
    }
    System.out.println("denied:");
    for (BgpRoute denied : result.getDenied().getAllRoutes()) {
      System.out.println(denied);
    }
  }
}
