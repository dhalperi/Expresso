package datamodel.vendor;

import controlplane.route.BgpRoute;
import controlplane.route.RoutingProtocol;

public class Huawei extends Vendor {
    public static Huawei HUAWEI = new Huawei();

    private Huawei() {}

    @Override
    public int getDefaultInternalPreference(RoutingProtocol protocol) {
        switch (protocol) {
            case CONNECTED:
                return 0;
            case OSPF:
                return 10;
            case ISIS_L1:
                return 15;
            case ISIS_L2:
                return 18;
            case STATIC:
            case LOCAL:
                return 60;
            case RIP:
                return 100;
            case OSPF_ASE:
            case OSPF_NSSA:
                return 150;
            case IBGP:
                return 200;
            case EBGP:
                return 20;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getDefaultExternalPreference(RoutingProtocol protocol) {
        switch (protocol) {
            case CONNECTED:
                return 0;
            case OSPF:
                return 10;
            case ISIS:
            case ISIS_L1: // todo
            case ISIS_L2: // todo
                return 15;
            case STATIC:
            case LOCAL:
                return 60;
            case RIP:
                return 100;
            case OSPF_ASE:
            case OSPF_NSSA:
                return 150;
            case IBGP:
            case EBGP:
                return 255;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getSourcePreference(BgpRoute.BgpRouteType type) {
        switch (type) {
            case AGGREGATE:
                return 5;
            case SUMMARY:
                return 4;
            case NETWORK:
                return 3;
            case IMPORT:
                return 2;
            default:
                return 1;
        }
    }
}
