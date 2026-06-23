package datamodel.routepolicy.match;

import controlplane.route.DynamicRoute;
import controlplane.route.Route;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathIntf;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasAsPath;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Collections;
import java.util.Set;

public class AllowAsLoop implements Match {
  public static AllowAsLoop INSTANCE = new AllowAsLoop();

  private AllowAsLoop() {}

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
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    // todo check correctness
    if (route instanceof HasAsPath) {
      RouteFilterResult<R> result = new RouteFilterResult<>(route);
      HasAsPath has = (HasAsPath) route;
      AsPathIntf asPath =
          has.getAsPath()
              .allowAsLoop(
                  environment.getLocalConfig().getLocalAS(),
                  environment.getLocalConfig().getIpv4UnicastAddressFamily().getAllowAsLoop());
      R cloned = Route.clone(route);
      ((HasAsPath) cloned).setAsPath(asPath);
      if (asPath != null) result.getPermitted().add(cloned);
      return result;
    } else {
      return RouteFilterResult.permitAll(route);
    }
  }
}
