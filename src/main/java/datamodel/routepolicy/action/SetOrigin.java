package datamodel.routepolicy.action;

import controlplane.route.BgpRoute;
import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasOrigin;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetOrigin implements Action {
  private final Long asNum;
  private final BgpRoute.OriginType origin;

  public SetOrigin(Long asNum, BgpRoute.OriginType origin) {
    this.asNum = asNum;
    this.origin = origin;
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasOrigin) {
      ((HasOrigin) route).setOrigin(origin);
    } else {
      throw new IllegalArgumentException("This type of route does not have origin");
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
    SetOrigin setOrigin = (SetOrigin) o;
    return Objects.equals(asNum, setOrigin.asNum) && origin == setOrigin.origin;
  }

  @Override
  public int hashCode() {
    return Objects.hash(asNum, origin);
  }
}
