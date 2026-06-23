package controlplane.route;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import controlplane.network.Interface;
import controlplane.route.builder.RouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.longestprefixmatch.LongestPrefixMatchItem;
import main.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Preference vs Administrative Distance:
 *
 * <p>According to <a
 * href="https://en.wikipedia.org/wiki/Administrative_distance">wiki-administrative-distance</a>,
 * they are the same thing.
 *
 * <p>However, for huawei and juniper devices, they have something like internal/external
 * preference.
 */
public abstract class Route implements Comparable<Route>, LongestPrefixMatchItem {
  private static final String PROP_PROTOCOL = "protocol";
  private static final String PROP_PREFIXES = "prefixes";
  private static final String PROP_PREFIXES_BDD = "prefixesBdd";
  private static final String PROP_NEXTHOP_IP = "nexthopIp";
  private static final String PROP_NEXTHOP_INTERFACE = "nexthopInterface";
  private static final String PROP_PREFERENCE = "preference";
  private static final String PROP_ADMIN = "admin";
  private static final String PROP_TAG = "tag";
  private static final String PROP_METRIC = "metric";

  /** the _prefixes can be null but the _prefixesBdd must be valid (i.e., >= 0) */
  @Nullable Set<Prefix> _prefixes;

  int _prefixesBdd;

  Ip _nextHopIp;
  Interface _nextHopInterface;

  /**
   * For Huawei/Juniper devices, set-preference will set this attribute (external preference). And
   * it takes no effect on Cisco devices.
   */
  int _preference;
  /**
   * For Cisco devices, set-admin will set this attribute. And it works as internal preference on
   * Huawei/Juniper devices.
   */
  int _admin;

  long _tag;

  public Route(
      @Nullable Set<Prefix> prefixes,
      int prefixesBdd,
      Ip nextHopIp,
      Interface nextHopInterface,
      int preference,
      int admin,
      long tag) {
    _prefixes = prefixes;
    _prefixesBdd = prefixesBdd;
    _nextHopIp = nextHopIp;
    _nextHopInterface = nextHopInterface;
    _preference = preference;
    _admin = admin;
    _tag = tag;
  }

  @JsonProperty(PROP_PROTOCOL)
  public abstract RoutingProtocol getRoutingProtocol();

  @JsonProperty(PROP_PREFIXES)
  public Set<Prefix> getPrefixes() {
    return _prefixes;
  }

  public void setPrefixes(Set<Prefix> prefixes) {
    _prefixes = prefixes;
  }

  @JsonProperty(PROP_PREFIXES_BDD)
  public int getPrefixesBdd() {
    return _prefixesBdd;
  }

  public void setPrefixesBdd(int prefixesBdd) {
    if (_prefixesBdd != prefixesBdd) {
      prefixRanges = null;
    }
    _prefixesBdd = prefixesBdd;
  }

  @JsonProperty(PROP_NEXTHOP_IP)
  public Ip getNextHopIp() {
    return _nextHopIp;
  }

  public void setNextHopIp(Ip nextHopIp) {
    _nextHopIp = nextHopIp;
  }

  @JsonProperty(PROP_NEXTHOP_INTERFACE)
  public Interface getNextHopInterface() {
    return _nextHopInterface;
  }

  public void setNextHopInterface(Interface nextHopInterface) {
    _nextHopInterface = nextHopInterface;
  }

  @JsonProperty(PROP_PREFERENCE)
  public int getPreference() {
    return _preference;
  }

  public void setPreference(int preference) {
    _preference = preference;
  }

  @JsonProperty(PROP_ADMIN)
  public int getAdmin() {
    return _admin;
  }

  public void setAdmin(int admin) {
    _admin = admin;
  }

  @JsonProperty(PROP_TAG)
  public long getTag() {
    return _tag;
  }

  public void setTag(long tag) {
    _tag = tag;
  }

  @JsonProperty(PROP_METRIC)
  public abstract long getMetric();

  public abstract RouteBuilder<?, ?> toBuilder();

  public Attributes.Builder toAttributesBuilder() {
    return Attributes.builder()
        .setNextHopIp(_nextHopIp)
        .setNextHopInterface(_nextHopInterface)
        .setPreference(_preference)
        .setAdmin(_admin)
        .setTag(_tag);
  }

  /**
   * To get attributes, all children class only have to override the {@link
   * Route#toAttributesBuilder()} method.
   *
   * @return {@link Attributes}
   */
  @JsonIgnore
  public final Attributes getAttributes() {
    return toAttributesBuilder().build();
  }

  /**
   * For Huawei/Juniper devices, first compare external preference {@link Route#_preference}, then
   * compare internal preference {@link Route#_admin}. For Cisco devices, only compare admin {@link
   * Route#_admin}.
   *
   * @param o the other route
   * @return -1/0/1 if the priority of this route is higher/the same/lower than the other route.
   *     This is different from {@link Integer#compareTo(Integer)} since we are using a {@link
   *     java.util.TreeMap} to hold routes in {@link controlplane.rib.Rib}.
   */
  @Override
  public int compareTo(@Nonnull Route o) {
    return Comparator.comparing(Route::getPreference)
        .thenComparing(Route::getAdmin)
        .thenComparing(Route::getMetric)
        .compare(this, o);
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  Set<PrefixRange> prefixRanges;

  public List<PrefixRange> getPrefixRanges() {
    prefixRanges =
        prefixRanges != null
            ? prefixRanges
            : Controller.bddManager
                .getBddPrefixWrapper()
                .extractPrefixRanges(
                    Controller.bddManager.getBddPrefixWrapper().eraseEnvc(_prefixesBdd));
    return prefixRanges.stream().sorted().collect(Collectors.toList());
  }

  public MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("prefixes", getPrefixRanges())
        .add("prefixesBdd", _prefixesBdd)
        .add("nextHopIp", _nextHopIp)
        .add("nextHopInterface", _nextHopInterface)
        .add("preference", _preference)
        .add("admin", _admin)
        .add("tag", Attributes.printTag(_tag));
  }

  public static <R extends Route> Collection<R> toConcreteRoutes(R route) {
    return route.getPrefixRanges().stream()
        .flatMap(pr -> pr.toPrefixes().stream())
        .map(
            pfx -> {
              R newRoute = Route.clone(route);
              newRoute.setPrefixesBdd(
                  Controller.bddManager.getBddPrefixWrapper().encodePrefix(pfx));
              newRoute.setPrefixes(ImmutableSet.of(pfx));
              return newRoute;
            })
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  /** I tried a lot of ways to avoid this ugly cast but failed :( */
  public static <R extends Route> R clone(@Nonnull R route) {
    return (R) route.toBuilder().build();
  }
}
