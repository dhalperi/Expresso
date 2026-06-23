package controlplane.route;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import controlplane.route.builder.OspfRouteBuilder;
import datamodel.path.Path;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.route.HasCost;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

public class OspfRoute extends DynamicRoute<OspfRouteBuilder, OspfRoute> implements HasCost {
  long _cost;

  public OspfRoute(
      Set<Prefix> prefixes,
      int prefixesBdd,
      Ip nextHopIp,
      Interface nextHopInterface,
      int preference,
      int admin,
      long tag,
      Path path,
      long cost) {
    super(prefixes, prefixesBdd, nextHopIp, nextHopInterface, preference, admin, tag, path);
    _cost = cost;
  }

  @Override
  public RoutingProtocol getRoutingProtocol() {
    return RoutingProtocol.OSPF;
  }

  @Override
  public long getMetric() {
    return _cost;
  }

  @Override
  @JsonIgnore
  public long getCost() {
    return _cost;
  }

  @Override
  public void setCost(long cost) {
    _cost = cost;
  }

  @Override
  public Attributes.Builder toAttributesBuilder() {
    return super.toAttributesBuilder().setPath(_path).setCost((int) _cost);
  }

  @Override
  public OspfRouteBuilder toBuilder() {
    return new OspfRouteBuilder()
        .setPrefixes(_prefixes)
        .setPrefixesBdd(_prefixesBdd)
        .setNextHopIp(_nextHopIp)
        .setNextHopInterface(_nextHopInterface)
        .setPreference(_preference)
        .setAdmin(_admin)
        .setTag(_tag)
        .setPath(_path)
        .setMetric(_cost);
  }

  @Override
  public int compareTo(@NotNull Route o) {
    if (o instanceof OspfRoute) {
      return Comparator.comparing(OspfRoute::getMetric).compare(this, (OspfRoute) o);
    } else {
      return super.compareTo(o);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OspfRoute)) {
      return false;
    }
    OspfRoute other = (OspfRoute) o;
    return _prefixesBdd == other._prefixesBdd
        && Objects.equals(_nextHopIp, other._nextHopIp)
        && Objects.equals(_nextHopInterface, other._nextHopInterface)
        && _preference == other._preference
        && _admin == other._admin
        && _tag == other._tag
        && Objects.equals(_path, other._path)
        && _cost == other._cost;
  }

  @Override
  public int hashCode() {
    int h;
    h = _prefixesBdd;
    h = h * 31 + _nextHopIp.hashCode();
    h = h * 31 + Objects.hashCode(_nextHopInterface);
    h = h * 31 + _preference;
    h = h * 31 + _admin;
    h = h * 31 + Long.hashCode(_tag);
    h = h * 31 + Objects.hashCode(_path);
    h = h * 31 + Long.hashCode(_cost);
    return h;
  }

  @Override
  public MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("cost", _cost);
  }
}
