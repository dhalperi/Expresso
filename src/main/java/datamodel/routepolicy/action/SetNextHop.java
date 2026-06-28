package datamodel.routepolicy.action;

import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.Ip;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SetNextHop implements Action {
  private final Router router;
  private final Ip nextHop;

  /**
   * If this is true, 1. Set the next hop address to the local address when this apply clause is
   * used by an export policy. 2. Set the next hop address to the peer address when this apply
   * clause is used by an import policy.
   */
  private final boolean peerAddress;

  private final boolean self;
  /**
   * If this is true, a blackhole route mark will be added to the route being modified by this
   * {@link Action}. If a route has the blackhole route mark, traffics matching it will be
   * discarded.
   */
  private final boolean blackhole;

  public SetNextHop(
      Router router, Ip nextHop, boolean peerAddress, boolean self, boolean blackhole) {
    this.router = router;
    this.nextHop = nextHop;
    this.peerAddress = peerAddress;
    this.self = self;
    this.blackhole = blackhole;
  }

  public static SetNextHop ip(Router router, Ip nextHop) {
    return new SetNextHop(router, nextHop, false, false, false);
  }

  public static SetNextHop peerAddress(Router router) {
    return new SetNextHop(router, null, true, false, false);
  }

  public static SetNextHop blackhole(Router router) {
    return new SetNextHop(router, null, false, false, true);
  }

  public static SetNextHop self(Router router) {
    return new SetNextHop(router, null, false, true, false);
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (peerAddress) {
      if (environment.isIn()) {
        route.setNextHopIp(environment.getLocalConfig().getRemoteIp());
      } else {
        route.setNextHopIp(environment.getLocalConfig().getLocalIp());
      }
    } else if (self) {
      if (!environment.isIn()) {
        route.setNextHopIp(environment.getLocalConfig().getLocalIp());
      }
    } else if (blackhole) {
      // Prefer an explicitly-named null/discard interface (Cisco-style); otherwise fall back to the
      // router's synthetic blackhole interface. Either way the next-hop interface is a blackhole, so
      // the route is dropped (see Interface#isBlackhole / RecursiveResolver).
      Interface null0 =
          ObjectUtils.firstNonNull(
              router.getInterface("NULL0"),
              router.getInterface("null_interface"),
              router.getInterface("dsc0"),
              router.getBlackhole());
      route.setNextHopInterface(null0);
    } else {
      route.setNextHopIp(nextHop);
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
    SetNextHop that = (SetNextHop) o;
    return peerAddress == that.peerAddress
        && self == that.self
        && blackhole == that.blackhole
        && Objects.equals(router, that.router)
        && Objects.equals(nextHop, that.nextHop);
  }

  @Override
  public int hashCode() {
    return Objects.hash(router, nextHop, peerAddress, self, blackhole);
  }
}
