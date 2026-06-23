package controlplane.route;

import controlplane.network.Interface;
import controlplane.route.builder.ConnectedRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;

import java.util.Objects;
import java.util.Set;

public class ConnectedRoute extends Route {
  public ConnectedRoute(
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
    return RoutingProtocol.CONNECTED;
  }

  @Override
  public long getMetric() {
    return 0L;
  }

  @Override
  public ConnectedRouteBuilder toBuilder() {
    return new ConnectedRouteBuilder()
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
    if (!(o instanceof ConnectedRoute)) {
      return false;
    }
    ConnectedRoute other = (ConnectedRoute) o;
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
