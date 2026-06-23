package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

public class MatchFalse implements Match {
  public static MatchFalse INSTANCE = new MatchFalse();

  private MatchFalse() {}

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return RouteFilterResult.denyAll(route);
  }
}
