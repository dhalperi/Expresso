package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.filterlist.FilterList;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Objects;
import java.util.Set;

public class MatchFilterList implements Match {
  private final String listName;
  private final FilterList list;

  public MatchFilterList(String listName, FilterList list) {
    this.listName = listName;
    this.list = list;
  }

  public String getListName() {
    return listName;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    return list.filter(route, environment);
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return list.collectPrefixRanges();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return list.collectCommunityRegexes();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return list.collectAsPathRegexes();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchFilterList that = (MatchFilterList) o;
    return Objects.equals(listName, that.listName) && Objects.equals(list, that.list);
  }

  @Override
  public int hashCode() {
    return Objects.hash(listName, list);
  }
}
