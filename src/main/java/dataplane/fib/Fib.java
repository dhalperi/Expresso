package dataplane.fib;

import controlplane.network.VirtualRouter;
import controlplane.rib.RecursiveResolver;
import controlplane.rib.Rib;
import controlplane.route.Route;
import datamodel.ipv4.Ip;
import datamodel.longestprefixmatch.LongestPrefixMatch;

import java.util.HashSet;
import java.util.Set;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc3222.html">RFC-3222</a>
 *
 * <p>The forwarding information base described a database indexing network prefixes versus router
 * port identifiers.
 *
 * <p>At minimum, it contains the interface identifier and next hop information for each reachable
 * destination network prefix.
 *
 * <p>It is distinct from {@link controlplane.rib.Rib}, which holds all routing information received
 * from routing peers.
 *
 * <p>It contains unique paths only (i.e., does not contain secondary paths).
 */
public class Fib {
  VirtualRouter vrf;
  Set<FibEntry> entries;

  public Fib(VirtualRouter vrf) {
    this.vrf = vrf;
    this.entries = new HashSet<>();
  }

  public VirtualRouter getVrf() {
    return vrf;
  }

  public Set<FibEntry> getEntries() {
    return entries;
  }

  public FibEntry longestPrefixMatch(Ip ip) {
    return LongestPrefixMatch.longestPrefixMatch(entries, ip);
  }

  public <R extends Route> void compute(Rib<R> rib) {
    rib.getAllRoutes()
        .forEach(
            route -> {
              Set<RecursiveResolver.ResolutionResult> set =
                  RecursiveResolver.resolveRoute(route, rib);
              set.forEach(
                  result ->
                      entries.add(
                          new FibEntry(result.getFinalNextHopIp(), result.getResolutionSteps())));
            });
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    entries.forEach(entry -> builder.append(entry.toString()).append("\n\n"));
    return builder.toString();
  }
}
