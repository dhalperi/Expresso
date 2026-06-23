package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.RoutingProtocol;
import controlplane.route.StaticRoute;
import main.Configuration;

public final class StaticRouteBuilder extends RouteBuilder<StaticRouteBuilder, StaticRoute> {
    @Override
    public StaticRoute build() {
        return new StaticRoute(
                getPrefixes(),
                getPrefixesBdd(),
                getNextHopIp(),
                getNextHopInterface(),
                getPreference(),
                getAdmin(),
                getTag()
        );
    }

    @Override
    public int getPreference() {
        return _preference == Attributes.UNSET_ROUTE_PREFERENCE ?
                (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(RoutingProtocol.STATIC)) : _preference;
    }

    @Override
    public int getAdmin() {
        return _admin == Attributes.UNSET_ROUTE_ADMIN ?
                (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(RoutingProtocol.STATIC)) : _admin;
    }

    @Override
    protected StaticRouteBuilder getThis() {
        return this;
    }
}
