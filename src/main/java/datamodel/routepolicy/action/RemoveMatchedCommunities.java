package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RemoveMatchedCommunities implements Action {
  private final boolean matchAll;
  private final List<CommunityRegex> regexes;

  public RemoveMatchedCommunities(boolean matchAll, List<CommunityRegex> regexes) {
    this.matchAll = matchAll;
    this.regexes = regexes;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return Collections.emptySet();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return new HashSet<>(regexes);
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return Collections.emptySet();
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    // todo
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RemoveMatchedCommunities that = (RemoveMatchedCommunities) o;
    return matchAll == that.matchAll && Objects.equals(regexes, that.regexes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matchAll, regexes);
  }
}
