package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetTag implements Action {
  private final long tag;

  public SetTag(long tag) {
    this.tag = tag;
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
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    route.setTag(tag);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetTag setTag = (SetTag) o;
    return tag == setTag.tag;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag);
  }
}
