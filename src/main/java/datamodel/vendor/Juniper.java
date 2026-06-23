package datamodel.vendor;

import controlplane.route.BgpRoute;
import controlplane.route.RoutingProtocol;

public class Juniper extends Vendor {
  public static Juniper JUNIPER = new Juniper();

  private Juniper() {}

  /**
   * <a
   * href="https://www.juniper.net/documentation/us/en/software/junos/routing-overview/bgp/topics/concept/routing-protocols-default-route-preference-values.html"></a>
   */
  @Override
  public int getDefaultInternalPreference(RoutingProtocol protocol) {
    switch (protocol) {
      case AGGREGATE:
        return 130;
      case CONNECTED:
        return 0;
      case OSPF:
        return 10;
      case ISIS_L1:
        return 15;
      case ISIS_L2:
        return 18;
      case ISIS_EL1:
        return 160;
      case ISIS_EL2:
        return 165;
      case STATIC:
        return 1;
      case LOCAL:
        return 60;
      case RIP:
        return 100;
      case IBGP:
      case EBGP:
        return 170;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public int getDefaultExternalPreference(RoutingProtocol protocol) {
    return getDefaultInternalPreference(protocol);
  }

  @Override
  public int getSourcePreference(BgpRoute.BgpRouteType type) {
    return 0;
  }
}
