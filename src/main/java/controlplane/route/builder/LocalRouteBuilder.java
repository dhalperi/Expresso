package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.LocalRoute;
import controlplane.route.RoutingProtocol;
import main.Configuration;

public class LocalRouteBuilder extends RouteBuilder<LocalRouteBuilder, LocalRoute> {
    @Override
    public LocalRoute build() {
        return new LocalRoute(
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
                (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(RoutingProtocol.LOCAL)) : _preference;
    }

    @Override
    public int getAdmin() {
        return _admin == Attributes.UNSET_ROUTE_ADMIN ?
                (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(RoutingProtocol.LOCAL)) : _admin;
    }

    @Override
    protected LocalRouteBuilder getThis() {
        return this;
    }
}
