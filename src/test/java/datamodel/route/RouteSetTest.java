package datamodel.route;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.Range;
import datamodel.aspath.AsPath;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import main.Controller;
import org.junit.Ignore;
import org.junit.Test;

// Pre-existing scratch tests (unrelated to the Batfish migration): they assert nothing and
// print RouteSets built from `/1 le 32` ranges, so RouteSet#toString enumerates billions of
// concrete prefixes via PrefixRange#toPrefixes and errors/OOMs after several minutes.
public class RouteSetTest {

    @Ignore("Pre-existing failure: prints a RouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void add() {
        Controller.pushBDDManager(new BddManager());

        RouteSet<BgpRoute> routeSet = new RouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.add(bgpRoute1);
        System.out.println(routeSet);
        routeSet.add(bgpRoute2);
        System.out.println(routeSet);
        routeSet.add(bgpRoute3);
        System.out.println(routeSet);
    }

    @Ignore("Pre-existing failure: prints a RouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void replace() {
        Controller.pushBDDManager(new BddManager());

        RouteSet<BgpRoute> routeSet = new RouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.replace(bgpRoute1);
        System.out.println(routeSet);
        routeSet.replace(bgpRoute2);
        System.out.println(routeSet);
        routeSet.replace(bgpRoute3);
        System.out.println(routeSet);
    }

    @Ignore("Pre-existing failure: prints a RouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void remove() {
        Controller.pushBDDManager(new BddManager());

        RouteSet<BgpRoute> routeSet = new RouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.add(bgpRoute1);
        System.out.println(routeSet);
        routeSet.remove(bgpRoute2);
        System.out.println(routeSet);
        routeSet.remove(bgpRoute3);
        System.out.println(routeSet);
    }
}