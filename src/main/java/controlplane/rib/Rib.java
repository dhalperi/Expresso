package controlplane.rib;

import com.google.common.collect.ImmutableList;
import controlplane.route.Route;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.longestprefixmatch.LongestPrefixMatch;
import dataplane.fib.Fib;
import javafx.util.Pair;
import main.Controller;
import util.BddUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Rib<R extends Route> {
    // todo support ecmp
    SortedMap<R, RibRouteSet<R>> rib;

    public Rib() {
        rib = new TreeMap<>();
    }

    public SortedMap<R, RibRouteSet<R>> getRib() {
        return rib;
    }

    public List<R> getAllRoutes() {
        return ImmutableList.copyOf(
                rib
                        .values()
                        .stream()
                        .flatMap(set -> set.getAllRoutes().stream())
                        .collect(Collectors.toList()));
    }

    /**
     * Just insert a {@link Route} into this {@link Rib}.
     * Do not update their {@link Route#getPrefixesBdd()}
     *
     * @param route the route to be inserted into this rib.
     */
    public <R1 extends R> void simpleInsert(R1 route) {
        if (rib.containsKey(route)) {
            rib.get(route).replace(route);
        }
        else {
            rib.put(Route.clone(route), new RibRouteSet<>(route));
        }
    }

    /**
     * Just insert a collection of routes into this rib.
     * Do not update their {@link Route#getPrefixesBdd()}
     *
     * @param routes a collection of routes to be inserted into this rib.
     */
    public <R1 extends R> void simpleInsert(Collection<R1> routes) {
        routes.forEach(this::simpleInsert);
    }

    /**
     * Insert a {@link Route} into this {@link Rib} and update their {@link Route#getPrefixesBdd()}
     *
     * @param route the route to be inserted into this rib.
     */
    public <R1 extends R> void sortInsert(R1 route) {
        simpleInsert(route);
        update();
    }

    /**
     * Insert a collection of routes into this rib and update their {@link Route#getPrefixesBdd()}
     *
     * @param routes a collection of routes to be inserted into this rib.
     */
    public <R1 extends R> void sortInsert(Collection<R1> routes) {
        simpleInsert(routes);
        update();
    }

    /**
     * {@link Rib#simpleInsert(Collection)} all routes in this rib into another rib.
     *
     * @param sourceRib the target rib
     */
    public <R1 extends R> void simpleInstall(Rib<R1> sourceRib) {
        simpleInsert(sourceRib.getAllRoutes());
    }

    /**
     * {@link Rib#sortInsert(Collection)} all routes in this rib into another rib.
     *
     * @param sourceRib the target rib
     */
    public <R1 extends R> void sortInstall(Rib<R1> sourceRib) {
        simpleInsert(sourceRib.getAllRoutes());
        update();
    }

    /**
     * Simply insert all routes in this rib into the given fib.
     *
     * @param targetFib the target fib
     */
    public void computeFib(Fib targetFib) {
        targetFib.compute(this);
    }

    /**
     * Find whether a given prefix can be matched by any route in this rib.
     *
     * @param prefix the prefix to be matched
     * @return true if there's at least one route that can match this prefix.
     */
    public boolean match(Prefix prefix) {
        int prefixBdd = Controller.bddManager.getBddPrefixWrapper().encodePrefix(prefix);
        return rib
                .values()
                .stream()
                .flatMap(set -> set.getAllRoutes().stream())
                .anyMatch(route -> Controller.bddManager.and(route.getPrefixesBdd(), prefixBdd) == prefixBdd);
    }

    /**
     * Find whether a given ip can be matched by any route in this rib.
     *
     * @param ip the ip to be matched
     * @return true if there's at least one route that can match this ip.
     */
    public R longestPrefixMatch(Ip ip) {
        return LongestPrefixMatch.longestPrefixMatch(getAllRoutes(), ip);
    }

    public List<R> getDelta() {
        return rib
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().getDelta().stream())
                .collect(Collectors.toList());
    }

    public void clean() {
        rib.entrySet().removeIf(entry -> entry.getValue().clean());
    }

    public String toString() {
        return rib
                .values()
                .stream()
                .map(RibRouteSet::toString)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Update the prefixesBdd of routes in a rib
     */
    public void update() {
        int matched = 0;
        for (Map.Entry<R, RibRouteSet<R>> entry : rib.entrySet()) {
            matched = multipathUpdate(matched, entry.getValue());
        }
    }

    public int multipathUpdate(int matched, RibRouteSet<R> equalCostRoutes) {
        List<Integer> list = new LinkedList<>();
        list.add(matched);

        for (Pair<R, RouteState> pair : equalCostRoutes.getAllRoutesWithState()) {
            R route = pair.getKey();
            RouteState state = pair.getValue();

            list.add(route.getPrefixesBdd());

            int newPrefixesBdd = 0;
            RouteState newState = state;
            if (matched != 1)  {
                newPrefixesBdd = Controller.bddManager.and(route.getPrefixesBdd(), Controller.bddManager.not(matched));
            }
            if (state == RouteState.INSERTED) {
                if (newPrefixesBdd == 0) {
                    newState = RouteState.EMPTY;
                }
            }
            else if (state != RouteState.REPLACED) {
                newState = route.getPrefixesBdd() != newPrefixesBdd ? RouteState.UPDATED : RouteState.UNCHANGED;
            }
            route.setPrefixesBdd(newPrefixesBdd);
            equalCostRoutes.setState(route.getAttributes(), newState);
        }

        return BddUtil.orInBatch(Controller.bddManager.getBDD(), list);
    }
}
