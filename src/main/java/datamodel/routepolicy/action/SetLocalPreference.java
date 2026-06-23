package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasLocalPreference;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetLocalPreference implements Action {
  private final int localPreference;
  private final boolean incremental;
  private final boolean decremental;

  private SetLocalPreference(int localPreference, boolean incremental, boolean decremental) {
    this.localPreference = localPreference;
    this.incremental = incremental;
    this.decremental = decremental;
  }

  public static SetLocalPreference replace(int localPreference) {
    return new SetLocalPreference(localPreference, false, false);
  }

  public static SetLocalPreference increase(int localPreference) {
    return new SetLocalPreference(localPreference, true, false);
  }

  public static SetLocalPreference decrease(int localPreference) {
    return new SetLocalPreference(localPreference, false, true);
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasLocalPreference) {
      int lp = localPreference;
      if (incremental) lp += ((HasLocalPreference) route).getLocalPreference();
      else if (decremental) lp -= ((HasLocalPreference) route).getLocalPreference();
      ((HasLocalPreference) route).setLocalPreference(lp);
    } else {
      throw new IllegalArgumentException("This type of route does not have local preference");
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
    SetLocalPreference that = (SetLocalPreference) o;
    return localPreference == that.localPreference
        && incremental == that.incremental
        && decremental == that.decremental;
  }

  @Override
  public int hashCode() {
    return Objects.hash(localPreference, incremental, decremental);
  }
}
