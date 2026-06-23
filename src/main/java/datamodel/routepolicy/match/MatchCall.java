package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.routepolicy.RoutePolicy;

import java.util.Objects;
import java.util.Set;

/** Used for {@link datamodel.routepolicy.NestedRoutePolicy}. */
public class MatchCall implements Match {
  private final RoutePolicy calledPolicy;

  public MatchCall(RoutePolicy calledPolicy) {
    this.calledPolicy = calledPolicy;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    boolean oldCallExprContext = environment.isCallExprContext();
    environment.setCallExprContext(true);
    RouteFilterResult<R> result = calledPolicy.filter(route, environment);
    environment.setCallExprContext(oldCallExprContext);
    return result;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return calledPolicy.collectPrefixRanges();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return calledPolicy.collectCommunityRegexes();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return calledPolicy.collectAsPathRegexes();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchCall matchCall = (MatchCall) o;
    return Objects.equals(calledPolicy, matchCall.calledPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calledPolicy);
  }

  @Override
  public String toString() {
    return "MatchCall{" + "calledPolicy=" + calledPolicy.getName() + '}';
  }
}
