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
 * Represents multiple match conditions in one <i>if-match</i> clause. A {@link Route} have to match
 * one of the {@link Match}s in this {@link MatchOne} to be permitted.
 */
public class MatchOne implements Match {
  private final List<Match> matches;

  public MatchOne(Match... matches) {
    this(Arrays.asList(matches));
  }

  public MatchOne(List<Match> matches) {
    this.matches = matches;
  }

  public List<Match> getMatches() {
    return matches;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return RouteFilter.matchOne(route, matches, environment);
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
    MatchOne matchOne = (MatchOne) o;
    return Objects.equals(matches, matchOne.matches);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matches);
  }
}
