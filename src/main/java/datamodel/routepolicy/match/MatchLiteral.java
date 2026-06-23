package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.community.CommunityRegexConjunction;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MatchLiteral implements Match {
  private final CanBeMatched condition;

  public MatchLiteral(CanBeMatched condition) {
    this.condition = condition;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return condition.filter(route, environment);
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    if (condition instanceof PrefixRange) {
      return Collections.singleton((PrefixRange) condition);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    if (condition instanceof CommunityRegex) {
      return Collections.singleton((CommunityRegex) condition);
    } else if (condition instanceof CommunityRegexConjunction) {
      return new HashSet<>(((CommunityRegexConjunction) condition).getCommunityRegexes());
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    if (condition instanceof AsPathRegex) {
      return Collections.singleton((AsPathRegex) condition);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchLiteral that = (MatchLiteral) o;
    return Objects.equals(condition, that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition);
  }
}
