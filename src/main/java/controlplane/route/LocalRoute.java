package controlplane.route;

import controlplane.network.Interface;
import controlplane.route.builder.LocalRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;

import java.util.Objects;
import java.util.Set;

public class LocalRoute extends Route {
  public LocalRoute(
      Set<Prefix> prefixes,
      int prefixesBdd,
      Ip nextHopIp,
      Interface nextHopInterface,
      int preference,
      int admin,
      long tag) {
    super(prefixes, prefixesBdd, nextHopIp, nextHopInterface, preference, admin, tag);
  }

  @Override
  public RoutingProtocol getRoutingProtocol() {
    return RoutingProtocol.LOCAL;
  }

  @Override
  public long getMetric() {
    return 0L;
  }

  @Override
  public LocalRouteBuilder toBuilder() {
    return new LocalRouteBuilder()
        .setPrefixes(_prefixes)
        .setPrefixesBdd(_prefixesBdd)
        .setNextHopIp(_nextHopIp)
        .setNextHopInterface(_nextHopInterface)
        .setPreference(_preference)
        .setAdmin(_admin)
        .setTag(_tag);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocalRoute)) {
      return false;
    }
    LocalRoute other = (LocalRoute) o;
    return _prefixesBdd == other._prefixesBdd
        && Objects.equals(_nextHopIp, other._nextHopIp)
        && Objects.equals(_nextHopInterface, other._nextHopInterface)
        && _preference == other._preference
        && _admin == other._admin
        && _tag == other._tag;
  }

  @Override
  public int hashCode() {
    int h;
    h = _prefixesBdd;
    h = h * 31 + _nextHopIp.hashCode();
    h = h * 31 + _nextHopInterface.hashCode();
    h = h * 31 + _preference;
    h = h * 31 + _admin;
    h = h * 31 + Long.hashCode(_tag);
    return h;
  }
}
