package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.RoutePolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

/** Used for {@link datamodel.routepolicy.NestedRoutePolicy}. */
public class MatchCallChain implements Match {
  private final List<MatchCall> callChain;

  public MatchCallChain(List<MatchCall> callChain) {
    this.callChain = callChain;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    RouteSet<R> fallThrough = new RouteSet<>(route);
    for (MatchCall matchCall : callChain) {
      RouteSet<R> fallThroughTmp = new RouteSet<>();
      for (R r : fallThrough.getAllRoutes()) {
        RouteFilterResult<R> resultTmp = matchCall.filter(r, environment);

        // exit routes
        SortedMap<Integer, RouteSet<R>> map1 = resultTmp.getExit();
        result.merge(map1);

        // return routes
        SortedMap<Integer, RouteSet<R>> map2 = resultTmp.getNonExitFallThrough();
        result.merge(RouteFilterResult.setReturnFalse(map2));

        // fall through routes
        Set<Integer> set = new HashSet<>(map1.keySet());
        set.addAll(map2.keySet());
        for (RouteSet<R> rs : resultTmp.getOtherResults(set).values()) {
          fallThroughTmp.addAll(rs);
        }
      }
      fallThrough = fallThroughTmp;
    }

    // default policy
    String defaultPolicyName = environment.getDefaultPolicy();
    RoutePolicy defaultPolicy = environment.getRouter().getRoutePolicy(defaultPolicyName);
    if (defaultPolicy != null) {
      for (R r : fallThrough.getAllRoutes()) {
        boolean oldCallExprContext = environment.isCallExprContext();
        boolean oldLocalDefaultAction = environment.isLocalDefaultAction();
        environment.setCallExprContext(true);
        RouteFilterResult<R> resultTmp = defaultPolicy.filter(r, environment);
        environment.setCallExprContext(oldCallExprContext);
        environment.setLocalDefaultAction(oldLocalDefaultAction);
        result.merge(RouteFilterResult.setReturnFalse(resultTmp.getResults()));
      }
    }

    return result;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return callChain.stream()
        .flatMap(call -> call.collectPrefixRanges().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return callChain.stream()
        .flatMap(call -> call.collectCommunityRegexes().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return callChain.stream()
        .flatMap(call -> call.collectAsPathRegexes().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchCallChain that = (MatchCallChain) o;
    return Objects.equals(callChain, that.callChain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(callChain);
  }
}
