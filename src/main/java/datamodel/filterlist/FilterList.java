package datamodel.filterlist;

import com.google.common.collect.ImmutableList;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.aspath.AsPathRegex;
import datamodel.collector.Collector;
import datamodel.community.CommunityRegex;
import datamodel.community.CommunityRegexConjunction;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import javafx.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <a
 * href="https://support.huawei.com/enterprise/en/doc/EDOC1000178171/4d8bdd30/ip-prefix-list"></a> A
 * list can contain multiple index entries, each of which corresponds to a filtering rule. It
 * matches routes against entries in ascending order of index number.
 */
public class FilterList implements Collector, RouteFilter {
  private final String name;
  private final List<Pair<CanBeMatched, Mode>> lines;
  private boolean used;

  public FilterList(String name, List<Pair<CanBeMatched, Mode>> lines) {
    this.name = name;
    this.lines = ImmutableList.copyOf(lines);
  }

  public String getName() {
    return name;
  }

  public List<Pair<CanBeMatched, Mode>> getLines() {
    return lines;
  }

  public boolean isUsed() {
    return used;
  }

  public void setUsed(boolean used) {
    this.used = used;
  }

  /**
   * If a route matches a permit entry, this route is permitted. If a route matches a deny entry,
   * this route is denied. If a route does not match any entry in the IP prefix list, this route is
   * denied.
   *
   * @return whether the route is permitted or denied by this filter list
   */
  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    RouteSet<R> remains = new RouteSet<>(route);
    for (Pair<CanBeMatched, Mode> line : lines) {
      RouteSet<R> remainsTmp = new RouteSet<>();
      for (R remain : remains.getAllRoutes()) {
        RouteFilterResult<R> resultTmp = line.getKey().filter(remain, environment);
        if (line.getValue() == Mode.PERMIT) {
          result.getPermitted().addAll(resultTmp.getPermitted());
        } else {
          result.getDenied().addAll(resultTmp.getPermitted());
        }
        remainsTmp.addAll(resultTmp.getDenied());
      }
      remains = remainsTmp;
    }
    // default deny all
    result.getDenied().addAll(remains);
    return result;
  }

  Set<PrefixRange> prefixRanges;

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return prefixRanges =
        (prefixRanges != null
            ? prefixRanges
            : lines.stream()
                .map(Pair::getKey)
                .filter(c -> c instanceof PrefixRange)
                .map(c -> (PrefixRange) c)
                .collect(Collectors.toSet()));
  }

  Set<CommunityRegex> communityRegexes;

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    if (communityRegexes != null) {
      return communityRegexes;
    } else {
      Set<CommunityRegex> set =
          lines.stream()
              .map(Pair::getKey)
              .filter(c -> c instanceof CommunityRegex)
              .map(c -> (CommunityRegex) c)
              .collect(Collectors.toSet());
      set.addAll(
          lines.stream()
              .map(Pair::getKey)
              .filter(c -> c instanceof CommunityRegexConjunction)
              .flatMap(c -> ((CommunityRegexConjunction) c).getCommunityRegexes().stream())
              .collect(Collectors.toSet()));
      return set;
    }
  }

  Set<AsPathRegex> asPathRegexes;

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return asPathRegexes =
        (asPathRegexes != null
            ? asPathRegexes
            : lines.stream()
                .map(Pair::getKey)
                .filter(c -> c instanceof AsPathRegex)
                .map(c -> (AsPathRegex) c)
                .collect(Collectors.toSet()));
  }

  Set<PrefixRange> permits;

  public Set<PrefixRange> collectPermits() {
    return permits =
        (permits != null
            ? permits
            : lines.stream()
                .filter(
                    line ->
                        (line.getKey() instanceof PrefixRange && line.getValue() == Mode.PERMIT))
                .map(line -> (PrefixRange) line.getKey())
                .collect(Collectors.toSet()));
  }

  Set<PrefixRange> denies;

  public Set<PrefixRange> collectDenies() {
    return denies =
        (denies != null
            ? denies
            : lines.stream()
                .filter(
                    line -> (line.getKey() instanceof PrefixRange && line.getValue() == Mode.DENY))
                .map(line -> (PrefixRange) line.getKey())
                .collect(Collectors.toSet()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilterList that = (FilterList) o;
    return Objects.equals(name, that.name) && Objects.equals(lines, that.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, lines);
  }
}
