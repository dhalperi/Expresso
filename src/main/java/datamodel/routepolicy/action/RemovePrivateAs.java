package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasAsPath;
import datamodel.route.RouteFilterEnvironment;

import java.util.Collections;
import java.util.Set;

public class RemovePrivateAs implements Action {
  public static RemovePrivateAs INSTANCE = new RemovePrivateAs();

  private RemovePrivateAs() {}

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
    if (route instanceof HasAsPath) {
      HasAsPath has = (HasAsPath) route;
      has.setAsPath(has.getAsPath().removePrivateAs());
    }
  }
}
