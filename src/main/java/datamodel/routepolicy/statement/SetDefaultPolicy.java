package datamodel.routepolicy.statement;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import org.batfish.datamodel.routing_policy.Result;

import java.util.Objects;

public class SetDefaultPolicy extends Statement {
  private final String defaultPolicy;

  public SetDefaultPolicy(String defaultPolicy) {
    this.defaultPolicy = defaultPolicy;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    environment.setDefaultPolicy(defaultPolicy);
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    result.getResults(new Result()).values().forEach(rs -> rs.add(route.toBuilder().build()));
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetDefaultPolicy that = (SetDefaultPolicy) o;
    return Objects.equals(defaultPolicy, that.defaultPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultPolicy);
  }

  @Override
  public String toString() {
    return "SetDefaultPolicy{" + "defaultPolicy='" + defaultPolicy + '\'' + '}';
  }
}
