package controlplane.route;

import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import controlplane.route.builder.IsisRouteBuilder;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.path.Path;
import org.batfish.datamodel.isis.IsisLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

public class IsisRoute extends DynamicRoute<IsisRouteBuilder, IsisRoute> {
  /** Default Isis route metric, unless one is explicitly specified */
  public static final long DEFAULT_METRIC = 10L;

  RoutingProtocol _protocol;
  String _area;
  boolean _attach;
  boolean _down;
  IsisLevel _level;
  long _metric;
  boolean _overload;
  String _systemId;

  public IsisRoute(
      Set<Prefix> prefixes,
      int prefixesBdd,
      Ip nextHopIp,
      Interface nextHopInterface,
      int preference,
      int admin,
      long tag,
      Path path,
      RoutingProtocol protocol,
      String area,
      boolean attach,
      boolean down,
      IsisLevel level,
      long metric,
      boolean overload,
      String systemId) {
    super(prefixes, prefixesBdd, nextHopIp, nextHopInterface, preference, admin, tag, path);
    _protocol = protocol;
    _area = area;
    _attach = attach;
    _down = down;
    _level = level;
    _metric = metric;
    _overload = overload;
    _systemId = systemId;
  }

  @Override
  public RoutingProtocol getRoutingProtocol() {
    return _protocol;
  }

  public String getArea() {
    return _area;
  }

  public boolean getAttach() {
    return _attach;
  }

  public boolean getDown() {
    return _down;
  }

  public IsisLevel getLevel() {
    return _level;
  }

  @Override
  public long getMetric() {
    return _metric;
  }

  public boolean getOverload() {
    return _overload;
  }

  public String getSystemId() {
    return _systemId;
  }

  @Override
  public IsisRouteBuilder toBuilder() {
    return new IsisRouteBuilder()
        .setPrefixes(_prefixes)
        .setPrefixesBdd(_prefixesBdd)
        .setNextHopIp(_nextHopIp)
        .setNextHopInterface(_nextHopInterface)
        .setPreference(_preference)
        .setAdmin(_admin)
        .setTag(_tag)
        .setPath(_path)
        .setProtocol(_protocol)
        .setArea(_area)
        .setAttach(_attach)
        .setDown(_down)
        .setLevel(_level)
        .setMetric(_metric)
        .setOverload(_overload)
        .setSystemId(_systemId);
  }

  @Override
  public int compareTo(@NotNull Route o) {
    if (o instanceof IsisRoute) {
      return Comparator.comparing(IsisRoute::getAdmin)
          .thenComparing(IsisRoute::levelCost)
          .thenComparing(IsisRoute::getOverload, Comparator.reverseOrder())
          .thenComparing(IsisRoute::getMetric)
          .compare(this, (IsisRoute) o);
    } else {
      return super.compareTo(o);
    }
  }

  private static int levelCost(IsisRoute isisRoute) {
    // Values returned are arbitrary, but L1 routes are preferred and must have lower cost than L2.
    switch (isisRoute.getLevel()) {
      case LEVEL_1:
        return 1;
      case LEVEL_2:
        return 2;
      default:
        throw new IllegalArgumentException(
            String.format("Invalid route level: %s", isisRoute.getLevel()));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IsisRoute)) {
      return false;
    }
    IsisRoute other = (IsisRoute) o;
    return _prefixesBdd == other._prefixesBdd
        && Objects.equals(_nextHopIp, other._nextHopIp)
        && Objects.equals(_nextHopInterface, other._nextHopInterface)
        && _preference == other._preference
        && _admin == other._admin
        && _tag == other._tag
        && Objects.equals(_path, other._path)
        && Objects.equals(_area, other._area)
        && _attach == other._attach
        && _down == other._down
        && _level == other._level
        && _metric == other._metric
        && _overload == other._overload
        && Objects.equals(_systemId, other._systemId);
  }

  @Override
  public int hashCode() {
    int h;
    h = _prefixesBdd;
    h = h * 31 + Objects.hashCode(_nextHopIp);
    h = h * 31 + Objects.hashCode(_nextHopInterface);
    h = h * 31 + _preference;
    h = h * 31 + _admin;
    h = h * 31 + Long.hashCode(_tag);
    h = h * 31 + Objects.hashCode(_path);
    h = h * 31 + Objects.hashCode(_area);
    h = h * 31 + Boolean.hashCode(_attach);
    h = h * 31 + Boolean.hashCode(_down);
    h = h * 31 + _level.hashCode();
    h = h * 31 + Long.hashCode(_metric);
    h = h * 31 + Boolean.hashCode(_overload);
    h = h * 31 + Objects.hashCode(_systemId);
    return h;
  }

  @Override
  public Attributes.Builder toAttributesBuilder() {
    return super.toAttributesBuilder()
        .setRoutingProtocol(_protocol)
        .setArea(_area)
        .setAttach(_attach)
        .setDown(_down)
        .setLevel(_level)
        .setMetric(_metric)
        .setOverload(_overload)
        .setSystemId(_systemId);
  }

  @Override
  public MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper()
        .add("protocol", _protocol)
        .add("area", _area)
        .add("attach", _attach)
        .add("down", _down)
        .add("level", _level)
        .add("metric", _metric)
        .add("overload", _overload)
        .add("systemId", _systemId);
  }
}
