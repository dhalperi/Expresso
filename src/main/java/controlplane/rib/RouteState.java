package controlplane.rib;

import controlplane.route.Route;

public enum RouteState {
    /**
     * Represents a route that is just inserted into a rib.
     */
    INSERTED,
    /**
     * Represents a route that is replaced by a new route with the same attributes.
     * These two routes have the same attributes but different {@link Route#getPrefixesBdd()}.
     */
    REPLACED,
    /**
     * Represents a route that its {@link Route#getPrefixesBdd()} changed while {@link Rib#update()}.
     */
    UPDATED,
    /**
     * Represents a route that is not {@link RouteState#INSERTED} or {@link RouteState#REPLACED} or {@link RouteState#UPDATED}.
     */
    UNCHANGED,
    /**
     * Represents a route that its {@link Route#getPrefixesBdd()} equals 0.
     */
    EMPTY
}
