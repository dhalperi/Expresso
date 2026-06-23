package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.ConnectedRoute;
import controlplane.route.RoutingProtocol;
import main.Configuration;

public final class ConnectedRouteBuilder extends RouteBuilder<ConnectedRouteBuilder, ConnectedRoute> {
    @Override
    public ConnectedRoute build() {
        return new ConnectedRoute(
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
                (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(RoutingProtocol.CONNECTED)) : _preference;
    }

    @Override
    public int getAdmin() {
        return _admin == Attributes.UNSET_ROUTE_ADMIN ?
                (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(RoutingProtocol.CONNECTED)) : _admin;
    }

    @Override
    protected ConnectedRouteBuilder getThis() {
        return this;
    }
}
