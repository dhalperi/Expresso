package datamodel.route;

import controlplane.route.Attributes;
import controlplane.route.Route;
import main.Controller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * One {@link Attributes} maps to one {@link Route}
 * Routes are ordered by insertion order.
 * @param <R>
 */
public class RouteSet<R extends Route> {
    public static <R extends Route> R add(R r1, R r2) {
        R r = Route.clone(r1);
        r.setPrefixesBdd(Controller.bddManager.or(r1.getPrefixesBdd(), r2.getPrefixesBdd()));
        return r;
    }

    public static <R extends Route> R remove(R r1, R r2) {
        R r = Route.clone(r1);
        r.setPrefixesBdd(Controller.bddManager.minus(r1.getPrefixesBdd(), r2.getPrefixesBdd()));
        return r;
    }

    protected LinkedList<Attributes> order;
    protected HashMap<Attributes, R> routes;

    /**
     * By default, if the route to be added into this set has the same attributes with an existing route in this set,
     * we OR {@link bdd.BddManager#or(int, int)} their prefixesBdd {@link Route#getPrefixesBdd()}.
     */
    public RouteSet() {
        this.order = new LinkedList<>();
        this.routes = new HashMap<>();
    }

    public RouteSet(Collection<R> routes) {
        this();
        routes.forEach(this::add);
    }

    @SafeVarargs
    public RouteSet(R... routes) {
        this(Arrays.asList(routes));
    }

    protected void add(Attributes attr, R route) {
        if (routes.containsKey(attr)) {
            R old = routes.get(attr);
            route = add(old, route);
            order.remove(attr);
        }
        routes.put(attr, route);
        order.addLast(attr);
    }

    /**
     * If the given route has the same attributes with a route that is already in this set,
     * combine their {@link Route#getPrefixesBdd()}.
     * @param route the route to be added into this {@link RouteSet}
     */
    public void add(R route) {
        add(route.getAttributes(), route);
    }

    public void addAll(Collection<R> routes) {
        routes.forEach(this::add);
    }

    public void addAll(RouteSet<R> another) {
        another.routes.forEach(this::add);
    }

    protected void replace(Attributes attr, R route) {
        route = Route.clone(route);
        if (routes.containsKey(attr)) {
            order.remove(attr);
        }
        routes.put(attr, route);
        order.addLast(attr);
    }

    public void replace(R route) {
        replace(route.getAttributes(), route);
    }

    public void replaceAll(Collection<R> routes) {
        routes.forEach(this::replace);
    }

    public void replaceAll(RouteSet<R> another) {
        another.routes.forEach(this::replace);
    }

    protected void remove(Attributes attr, R route) {
        if (routes.containsKey(attr)) {
            R old = routes.get(attr);
            route = remove(old, route);
            routes.put(attr, route);
        }
    }

    public void remove(R route) {
        remove(route.getAttributes(), route);
    }

    public void removeAll(Collection<R> routes) {
        routes.forEach(this::remove);
    }

    public void removeAll(RouteSet<R> another) {
        another.routes.forEach(this::remove);
    }

    /**
     * @return A list of routes in the order of insertion.
     */
    public List<R> getAllRoutes() {
        return order.stream().map(attr -> routes.get(attr)).collect(Collectors.toList());
    }

    public void clear() {
        order.clear();
        routes.clear();
    }

    public boolean isEmpty() {
        return order.isEmpty();
    }

    @Override
    public String toString() {
        List<String> list = order
                .stream()
                .map(attr -> routes.get(attr))
                .filter(route -> !route.getPrefixRanges().isEmpty())
                .flatMap(route -> Route.toConcreteRoutes(route).stream())
                .map(Route::toString)
                .collect(Collectors.toList());
        return list.isEmpty() ? null :
                String.join("\n", "RouteSet{", String.join("\n", list), "}");
    }
}
