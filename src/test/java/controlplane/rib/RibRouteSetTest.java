package controlplane.rib;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.Route;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.Range;
import datamodel.aspath.AsPath;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import javafx.util.Pair;
import main.Controller;
import org.junit.Ignore;
import org.junit.Test;

// Pre-existing scratch tests (unrelated to the Batfish migration): they assert nothing and print
// RibRouteSets built from `/1 le 32` ranges, so Route#toString enumerates billions of concrete
// prefixes via PrefixRange#toPrefixes and errors/OOMs.
public class RibRouteSetTest {

    public static <R extends Route> void print(RibRouteSet<R> ribRouteSet) {
        for (Pair<R, RouteState> pair : ribRouteSet.getAllRoutesWithState()) {
            System.out.println(pair.getKey());
            System.out.println(pair.getValue());
            System.out.println();
        }
        System.out.println("----------------------------------------");
    }

    @Ignore("Pre-existing failure: prints a RibRouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void add() {
        Controller.pushBDDManager(new BddManager());

        RibRouteSet<BgpRoute> routeSet = new RibRouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.add(bgpRoute1);
        print(routeSet);
        routeSet.add(bgpRoute2);
        print(routeSet);
        routeSet.add(bgpRoute3);
        print(routeSet);
    }

    @Ignore("Pre-existing failure: prints a RibRouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void replace() {
        Controller.pushBDDManager(new BddManager());

        RibRouteSet<BgpRoute> routeSet = new RibRouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.replace(bgpRoute1);
        print(routeSet);
        routeSet.replace(bgpRoute2);
        print(routeSet);
        routeSet.replace(bgpRoute3);
        print(routeSet);
    }

    @Ignore("Pre-existing failure: prints a RibRouteSet over /1 le 32; toString OOMs enumerating prefixes")
    @Test
    public void remove() {
        Controller.pushBDDManager(new BddManager());

        RibRouteSet<BgpRoute> routeSet = new RibRouteSet<>();

        PrefixRange pr1 = PrefixRange.of(Prefix.of("128.0.0.0/1"), new Range<>(1, 32));
        PrefixRange pr2 = PrefixRange.of(Prefix.of("192.0.0.0/2"), new Range<>(2, 32));
        PrefixRange pr3 = PrefixRange.of(Prefix.of("0.0.0.0/1"), new Range<>(1, 32));

        BgpRouteBuilder builder = new BgpRouteBuilder();
        BgpRoute bgpRoute1 = builder.setPrefixesBdd(pr1.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute2 = builder.setPrefixesBdd(pr2.toBdd()).setAsPath(AsPath.ofSingletonAsSets(1L)).build();
        BgpRoute bgpRoute3 = builder.setPrefixesBdd(pr3.toBdd()).setAsPath(AsPath.ofSingletonAsSets(2L)).build();

        routeSet.add(bgpRoute1);
        print(routeSet);
        routeSet.remove(bgpRoute2);
        print(routeSet);
        routeSet.remove(bgpRoute3);
        print(routeSet);
    }
}