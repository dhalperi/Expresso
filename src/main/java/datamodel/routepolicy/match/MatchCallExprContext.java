package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import org.batfish.datamodel.routing_policy.Result;

/** Used for {@link datamodel.routepolicy.NestedRoutePolicy}. */
public class MatchCallExprContext implements Match {
  public static MatchCallExprContext INSTANCE = new MatchCallExprContext();

  private MatchCallExprContext() {}

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    result
        .getResults(new Result(environment.isCallExprContext()))
        .values()
        .forEach(rs -> rs.add(route.toBuilder().build()));
    return result;
  }
}
