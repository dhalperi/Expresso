package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Set;

public class SetBackupNextHop implements Action {
  /**
   * Do nothing, because we do not consider frr currently.
   *
   * @param route the route to be set backup next hop.
   */
  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {}

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
}
