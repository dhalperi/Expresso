package datamodel.routepolicy.statement;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.StaticAction;
import org.batfish.datamodel.routing_policy.Result;

import java.util.Objects;
import java.util.Set;

public class ActionStatement extends Statement {
  private final Action action;

  public ActionStatement(Action action) {
    this.action = action;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return action.collectPrefixRanges();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return action.collectCommunityRegexes();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return action.collectAsPathRegexes();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ActionStatement that = (ActionStatement) o;
    return Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action);
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);

    R cloned = route.toBuilder().build();
    if (action instanceof StaticAction) {
      StaticAction sa = (StaticAction) action;
      environment.setResult(result);
      sa.act(cloned, environment);
      environment.setResult(null);
    } else {
      action.act(cloned, environment);
      result.getResults(new Result()).values().forEach(rs -> rs.add(cloned.toBuilder().build()));
    }

    return result;
  }

  @Override
  public String toString() {
    return "ActionStatement{" + "action=" + action + '}';
  }
}
