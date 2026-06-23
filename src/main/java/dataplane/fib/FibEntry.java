package dataplane.fib;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import controlplane.network.Interface;
import controlplane.route.Route;
import datamodel.ipv4.Ip;
import datamodel.longestprefixmatch.LongestPrefixMatchItem;
import main.Controller;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc3222.html#section-5.4">RFC-3222</a> <br>
 * A single entry within a FIB ({@link Fib}). It consists of the minimum amount of information
 * necessary to make a forwarding decision on a particular packet. The typical components within a
 * FIB entry are a network prefix, a router port identifier and next hop information. It is an entry
 * that the router can and does use to forward packets.
 */
public class FibEntry implements Comparable<FibEntry>, LongestPrefixMatchItem {
  final int prefixesBdd;
  final Ip nextHopIp;
  final Interface nextHopInterface;
  final List<Route> resolutionSteps;

  /**
   * @param nextHopIp final next-hop-ip
   * @param resolutionSteps resolution steps For a route with only next-hop-ip (e.g. {@link
   *     controlplane.route.StaticRoute}) we have to do recursive look up on its next-hop-ip, then
   *     create a forwarding rule according to its resolution steps and final next-hop-ip.
   */
  public FibEntry(Ip nextHopIp, List<Route> resolutionSteps) {
    this.prefixesBdd = resolutionSteps.get(0).getPrefixesBdd();
    this.nextHopIp = nextHopIp;
    this.nextHopInterface = Iterables.getLast(resolutionSteps).getNextHopInterface();
    this.resolutionSteps = resolutionSteps;
  }

  public int getPrefixesBdd() {
    return prefixesBdd;
  }

  public Ip getNextHopIp() {
    return nextHopIp;
  }

  public Interface getNextHopInterface() {
    return nextHopInterface;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FibEntry fibEntry = (FibEntry) o;
    return prefixesBdd == fibEntry.prefixesBdd
        && Objects.equals(nextHopIp, fibEntry.nextHopIp)
        && Objects.equals(nextHopInterface, fibEntry.nextHopInterface)
        && Objects.equals(resolutionSteps, fibEntry.resolutionSteps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prefixesBdd, nextHopIp, nextHopInterface, resolutionSteps);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add(
            "prefixes",
            Controller.bddManager.getBddPrefixWrapper().extractPrefixRanges(prefixesBdd))
        // .add("prefixesBdd", prefixesBdd)
        // .add("nextHopIp", nextHopIp)
        .add("nextHopInterface", nextHopInterface)
        .add("resolutionSteps", resolutionSteps)
        .toString();
  }

  @Override
  public int compareTo(@Nonnull FibEntry another) {
    return Comparator.comparing(FibEntry::getNextHopIp)
        .thenComparing(rule -> rule.nextHopInterface.getInterfaceName())
        .compare(this, another);
  }
}
