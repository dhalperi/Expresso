package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;

import java.util.Objects;
import java.util.Set;

public class MatchNot implements Match {
  private final Match match;

  public MatchNot(Match match) {
    this.match = match;
  }

  public Match getMatch() {
    return match;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = match.filter(route, environment);
    RouteSet<R> permitted = result.getPermitted();
    result.setPermitted(result.getDenied());
    result.setDenied(permitted);
    return result;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return match.collectPrefixRanges();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return match.collectCommunityRegexes();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return match.collectAsPathRegexes();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchNot matchNot = (MatchNot) o;
    return Objects.equals(match, matchNot.match);
  }

  @Override
  public int hashCode() {
    return Objects.hash(match);
  }
}
