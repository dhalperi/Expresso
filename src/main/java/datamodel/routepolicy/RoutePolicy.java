package datamodel.routepolicy;

import com.google.common.collect.ImmutableSortedMap;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.collector.Collector;
import datamodel.community.CommunityRegex;
import datamodel.filterlist.Mode;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;

import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

public class RoutePolicy implements Collector, RouteFilter {
  public static RoutePolicy PERMIT_ALL = new RoutePolicy("PERMIT_ALL", Node.PERMIT_ALL);
  public static RoutePolicy DENY_ALL = new RoutePolicy("DENY_ALL", Node.DENY_ALL);
  public static RoutePolicy REMOVE_PRIVATE_AS =
      new RoutePolicy("REMOVE_PRIVATE_AS", Node.REMOVE_PRIVATE_AS);
  public static RoutePolicy ALLOW_AS_LOOP = new RoutePolicy("ALLOW_AS_LOOP", Node.ALLOW_AS_LOOP);

  private final String name;
  private final SortedMap<Integer, Node> nodes;
  boolean used;

  protected RoutePolicy(String name) {
    this.name = name;
    this.nodes = ImmutableSortedMap.of();
  }

  public RoutePolicy(String name, Node... nodes) {
    this.name = name;
    ImmutableSortedMap.Builder<Integer, Node> builder = ImmutableSortedMap.naturalOrder();
    for (int i = 0; i < nodes.length; i++) {
      builder.put(i, nodes[i]);
    }
    this.nodes = builder.build();
  }

  public RoutePolicy(String name, SortedMap<Integer, Node> nodes) {
    this.name = name;
    this.nodes = nodes instanceof ImmutableSortedMap ? nodes : ImmutableSortedMap.copyOf(nodes);
  }

  public String getName() {
    return name;
  }

  public SortedMap<Integer, Node> getNodes() {
    return nodes;
  }

  public boolean isUsed() {
    return used;
  }

  public void setUsed(boolean used) {
    this.used = used;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    RouteSet<R> remains = new RouteSet<>(route);
    for (Node node : nodes.values()) {
      RouteSet<R> remainsTmp = new RouteSet<>();
      for (R remain : remains.getAllRoutes()) {
        RouteFilterResult<R> resultTmp = node.filter(remain, environment);
        if (node.mode == Mode.PERMIT) {
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
    if (prefixRanges == null) {
      prefixRanges =
          nodes.values().stream()
              .flatMap(node -> node.collectPrefixRanges().stream())
              .collect(Collectors.toSet());
    }
    return prefixRanges;
  }

  Set<CommunityRegex> communityRegexes;

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    if (communityRegexes == null) {
      communityRegexes =
          nodes.values().stream()
              .flatMap(node -> node.collectCommunityRegexes().stream())
              .collect(Collectors.toSet());
    }
    return communityRegexes;
  }

  Set<AsPathRegex> asPathRegexes;

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    if (asPathRegexes == null) {
      asPathRegexes =
          nodes.values().stream()
              .flatMap(node -> node.collectAsPathRegexes().stream())
              .collect(Collectors.toSet());
    }
    return asPathRegexes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoutePolicy that = (RoutePolicy) o;
    return Objects.equals(name, that.name) && Objects.equals(nodes, that.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, nodes);
  }
}
