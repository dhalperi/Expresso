package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetPreference implements Action {
  private final int preference;

  public SetPreference(int preference) {
    this.preference = preference;
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    route.setPreference(preference);
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
    SetPreference that = (SetPreference) o;
    return preference == that.preference;
  }

  @Override
  public int hashCode() {
    return Objects.hash(preference);
  }
}
