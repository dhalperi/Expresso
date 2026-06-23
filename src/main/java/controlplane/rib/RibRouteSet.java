package controlplane.rib;

import controlplane.route.Attributes;
import controlplane.route.Route;
import datamodel.route.RouteSet;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class RibRouteSet<R extends Route> extends RouteSet<R> {
    HashMap<Attributes, RouteState> states;

    public RibRouteSet() {
        super();
        this.states = new HashMap<>();
    }

    public RibRouteSet(Collection<R> routes) {
        this();
        routes.forEach(this::replace);
    }

    @SafeVarargs
    public RibRouteSet(R... routes) {
        this(Arrays.asList(routes));
    }

    @Override
    protected void add(Attributes attr, R route) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void replace(Attributes attr, R route) {
        route = Route.clone(route);
        if (routes.containsKey(attr)) {
            R old = routes.get(attr);
            if (old.getPrefixesBdd() != route.getPrefixesBdd()) {
                order.remove(attr);
                order.addLast(attr);
                routes.put(attr, route);
                states.put(attr, RouteState.REPLACED);
            }
        }
        else {
            order.addLast(attr);
            routes.put(attr, route);
            states.put(attr, RouteState.INSERTED);
        }
    }

    @Override
    protected void remove(Attributes attr, R route) {
        throw new UnsupportedOperationException();
    }

    public void setState(Attributes attr, RouteState state) {
        states.put(attr, state);
    }

    public List<Pair<R, RouteState>> getAllRoutesWithState() {
        return order.stream().map(attr -> new Pair<>(routes.get(attr), states.get(attr))).collect(Collectors.toList());
    }

    public List<R> getDelta() {
        return order
                .stream()
                .filter(attr -> states.get(attr) != RouteState.UNCHANGED && states.get(attr) != RouteState.EMPTY)
                .map(attr -> routes.get(attr))
                .collect(Collectors.toList());
    }

    /**
     * Sweep out {@link RouteState#EMPTY} routes and set all other routes' state to {@link RouteState#UNCHANGED}
     * @return true if empty after cleaning otherwise false
     */
    public boolean clean() {
        Iterator<Map.Entry<Attributes, RouteState>> itr = states.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Attributes, RouteState> entry = itr.next();
            Attributes attr = entry.getKey();
            if (routes.get(attr).getPrefixesBdd() == 0) {
                order.remove(attr);
                routes.remove(attr);
                itr.remove();
            }
            else {
                entry.setValue(RouteState.UNCHANGED);
            }
        }
        return states.isEmpty();
    }
}
