package datamodel.routepolicy.action;

import controlplane.process.bgp.BgpPeerConfig;
import controlplane.route.Route;
import datamodel.collector.Collector;
import datamodel.route.RouteFilterEnvironment;

public interface Action extends Collector {
  /**
   * @param route the route to be modified by this {@link Action}
   * @param environment (1) environment.in true for import route policy and false for export route
   *     policy, used by {@link SetNextHop}; (2) environment.localConfig local {@link
   *     BgpPeerConfig}, used by {@link SetNextHop}
   */
  <R extends Route> void act(R route, RouteFilterEnvironment<R> environment);
}
