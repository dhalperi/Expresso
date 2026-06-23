package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.Route;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents multiple <i>if-match</i> clauses in one node of a route policy. A {@link Route} have
 * to match all {@link MatchOne} in this {@link MatchAll} to be permitted.
 */
public class MatchAll implements Match {
  private final List<MatchOne> matches;

  public MatchAll(MatchOne... matches) {
    this(Arrays.asList(matches));
  }

  public MatchAll(List<MatchOne> matches) {
    this.matches = matches;
  }

  public List<MatchOne> getMatchOnes() {
    return matches;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return RouteFilter.matchAll(route, matches, environment);
  }

  Set<PrefixRange> prefixRanges;

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    if (prefixRanges == null) {
      prefixRanges =
          matches.stream()
              .map(Match::collectPrefixRanges)
              .filter(Objects::nonNull)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
    }
    return prefixRanges;
  }

  Set<CommunityRegex> communityRegexes;

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    if (communityRegexes == null) {
      communityRegexes =
          matches.stream()
              .map(Match::collectCommunityRegexes)
              .filter(Objects::nonNull)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
    }
    return communityRegexes;
  }

  Set<AsPathRegex> asPathRegexes;

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    if (asPathRegexes == null) {
      asPathRegexes =
          matches.stream()
              .map(Match::collectAsPathRegexes)
              .filter(Objects::nonNull)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
    }
    return asPathRegexes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchAll matchAll = (MatchAll) o;
    return Objects.equals(matches, matchAll.matches);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matches);
  }
}
