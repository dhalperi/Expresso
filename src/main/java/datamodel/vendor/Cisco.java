package datamodel.vendor;

import controlplane.route.BgpRoute;
import controlplane.route.RoutingProtocol;

public class Cisco extends Vendor {
    public static Cisco CISCO = new Cisco();

    private Cisco() {}

    @Override
    public int getDefaultInternalPreference(RoutingProtocol protocol) {
        return 0;
    }

    @Override
    public int getDefaultExternalPreference(RoutingProtocol protocol) {
        return 0;
    }

    @Override
    public int getSourcePreference(BgpRoute.BgpRouteType type) {
        switch (type) {
            case NETWORK:
            case IMPORT:
                return 3;
            case AGGREGATE:
            case SUMMARY:
                return 2;
            default:
                return 1;
        }
    }
}
