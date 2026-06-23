package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasWeight;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetWeight implements Action {
  private final int weight;

  public SetWeight(int weight) {
    this.weight = weight;
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasWeight) {
      ((HasWeight) route).setWeight(weight);
    } else {
      throw new IllegalArgumentException("This type of route does not have weight");
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
    SetWeight setWeight = (SetWeight) o;
    return weight == setWeight.weight;
  }

  @Override
  public int hashCode() {
    return Objects.hash(weight);
  }
}
