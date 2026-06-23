package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.OspfRoute;
import controlplane.route.RoutingProtocol;
import main.Configuration;

public final class OspfRouteBuilder extends DynamicRouteBuilder<OspfRouteBuilder, OspfRoute> {
    @Override
    protected OspfRouteBuilder getThis() {
        return this;
    }

    @Override
    public OspfRoute build() {
        return new OspfRoute(
                getPrefixes(),
                getPrefixesBdd(),
                getNextHopIp(),
                getNextHopInterface(),
                getPreference(),
                getAdmin(),
                getTag(),
                getPath(),
                getMetric()
        );
    }

    @Override
    public int getPreference() {
        return _preference == Attributes.UNSET_ROUTE_PREFERENCE ?
                (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(RoutingProtocol.OSPF)) : _preference;
    }

    @Override
    public int getAdmin() {
        return _admin == Attributes.UNSET_ROUTE_ADMIN ?
                (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(RoutingProtocol.OSPF)) : _admin;
    }
}
