package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasCost;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetCost implements Action {
  private final boolean incremental;
  private final boolean decremental;
  private final long cost;

  private SetCost(boolean incremental, boolean decremental, long cost) {
    this.incremental = incremental;
    this.decremental = decremental;
    this.cost = cost;
  }

  public static SetCost replace(long cost) {
    return new SetCost(false, false, cost);
  }

  public static SetCost increase(long cost) {
    return new SetCost(true, false, cost);
  }

  public static SetCost decrease(long cost) {
    return new SetCost(false, true, cost);
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasCost) {
      long c = cost;
      if (incremental) {
        c += ((HasCost) route).getCost();
      } else if (decremental) {
        c = ((HasCost) route).getCost() - c;
      }
      ((HasCost) route).setCost(c);
    } else {
      throw new IllegalArgumentException("This type of route does not have cost");
    }
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return Collections.emptySet();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return Collections.emptySet();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return Collections.emptySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetCost setCost = (SetCost) o;
    return incremental == setCost.incremental
        && decremental == setCost.decremental
        && cost == setCost.cost;
  }

  @Override
  public int hashCode() {
    return Objects.hash(incremental, decremental, cost);
  }
}
