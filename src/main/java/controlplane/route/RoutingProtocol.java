package controlplane.route;

import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

public enum RoutingProtocol implements CanBeMatched {
  AGGREGATE,
  CONNECTED,
  STATIC,
  LOCAL,
  BGP,
  IBGP,
  EBGP,
  OSPF,
  OSPF_ASE,
  OSPF_NSSA,
  RIP,
  ISIS,
  ISIS_L1,
  ISIS_L2,
  ISIS_EL1,
  ISIS_EL2;

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    R cloned = route.toBuilder().build();
    if (route.getRoutingProtocol() == this) result.getPermitted().add(cloned);
    else result.getDenied().add(cloned);
    return result;
  }
}
