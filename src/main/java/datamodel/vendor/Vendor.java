package datamodel.vendor;

import controlplane.route.BgpRoute;
import controlplane.route.RoutingProtocol;

public abstract class Vendor {
    public abstract int getDefaultInternalPreference(RoutingProtocol protocol);

    public abstract int getDefaultExternalPreference(RoutingProtocol protocol);

    public abstract int getSourcePreference(BgpRoute.BgpRouteType type);

    /**
     * Prefer route from EBGP peers to route from IBGP peers.
     */
    public int getPeerPreference(BgpRoute.BgpRouteType type) {
        switch (type) {
            case AGGREGATE:
            case SUMMARY:
            case NETWORK:
            case IMPORT:
                return 3;
            case FROM_EBGP_NEIGHBOR:
                return 2;
            default:
                return 1;
        }
    }
}
