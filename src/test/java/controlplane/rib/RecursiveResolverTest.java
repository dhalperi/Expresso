package controlplane.rib;

import bdd.BddManager;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.route.BgpRoute;
import controlplane.route.ConnectedRoute;
import controlplane.route.Route;
import controlplane.route.builder.BgpRouteBuilder;
import controlplane.route.builder.ConnectedRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import main.Controller;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class RecursiveResolverTest {

    @Test
    public void resolveRoute() {
        Controller.pushBDDManager(new BddManager());

        Interface.Builder intfBuilder = Interface.builder();
        Interface intf1 = intfBuilder.setInterfaceName(new InterfaceName("r1", "intf1")).build();
        Interface intf2 = intfBuilder.setInterfaceName(new InterfaceName("r1", "intf2")).build();

        BgpRouteBuilder builder = new BgpRouteBuilder();
        // route with nexthop interface
        BgpRoute toResolve1 = builder
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("192.168.1.0/24")))
                .setNextHopIp(Ip.AUTO)
                .setNextHopInterface(intf1)
                .build();
        // toResolve3 -> bgpRoute1 -> failed
        BgpRoute toResolve2 = builder
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("192.168.2.0/24")))
                .setNextHopIp(Ip.of("176.28.211.47"))
                .setNextHopInterface(null)
                .build();
        // toResolve3 -> bgpRoute2 -> connectedRoute
        BgpRoute toResolve3 = builder
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("192.168.3.0/24")))
                .setNextHopIp(Ip.of("7.241.194.242"))
                .setNextHopInterface(null)
                .build();

        BgpRoute bgpRoute1 = builder
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("176.28.211.44/30")))
                // this next hop ip doesn't match the connected route
                .setNextHopIp(Ip.of("125.254.16.192"))
                .setNextHopInterface(null)
                .build();
        BgpRoute bgpRoute2 = builder
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("7.241.194.240/30")))
                // this next hop ip matches the connected route
                .setNextHopIp(Ip.of("125.254.16.225"))
                .setNextHopInterface(null)
                .build();
        ConnectedRoute connectedRoute = new ConnectedRouteBuilder()
                .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(Prefix.of("125.254.16.224/30")))
                .setNextHopIp(Ip.AUTO)
                .setNextHopInterface(intf2)
                .build();

        Rib<Route> rib = new Rib<>();
        rib.simpleInsert(bgpRoute1);
        rib.simpleInsert(bgpRoute2);
        rib.simpleInsert(connectedRoute);

        Set<RecursiveResolver.ResolutionResult> results1 = RecursiveResolver.resolveRoute(toResolve1, rib);
        Set<RecursiveResolver.ResolutionResult> results2 = RecursiveResolver.resolveRoute(toResolve2, rib);
        Set<RecursiveResolver.ResolutionResult> results3 = RecursiveResolver.resolveRoute(toResolve3, rib);
        printResolutionResult(results1);
        printResolutionResult(results2);
        printResolutionResult(results3);
    }

    public static void printResolutionResult(Set<RecursiveResolver.ResolutionResult> results) {
        System.out.println("--------------------------------");
        System.out.println(
                results.stream()
                        .map(result -> result.resolutionSteps.stream().map(Route::toString).collect(Collectors.joining("\n")))
                        .collect(Collectors.joining("\n\n"))
        );
        System.out.println("--------------------------------");
    }
}