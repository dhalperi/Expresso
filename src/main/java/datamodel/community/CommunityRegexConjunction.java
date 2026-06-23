package datamodel.community;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CommunityRegexConjunction implements CanBeMatched {
  /** All regexes should be matched. */
  List<CommunityRegex> communityRegexes;

  public CommunityRegexConjunction(CommunityRegex... regexes) {
    this(Arrays.asList(regexes));
  }

  public CommunityRegexConjunction(List<CommunityRegex> communityRegexes) {
    this.communityRegexes = communityRegexes;
  }

  public List<CommunityRegex> getCommunityRegexes() {
    return communityRegexes;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return RouteFilter.matchAll(route, communityRegexes, environment);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommunityRegexConjunction that = (CommunityRegexConjunction) o;
    return Objects.equals(communityRegexes, that.communityRegexes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(communityRegexes);
  }
}
