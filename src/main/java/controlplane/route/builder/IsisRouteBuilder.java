package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.IsisRoute;
import controlplane.route.RoutingProtocol;
import main.Configuration;
import org.batfish.datamodel.isis.IsisLevel;

import static java.util.Objects.requireNonNull;

public class IsisRouteBuilder extends DynamicRouteBuilder<IsisRouteBuilder, IsisRoute> {
  RoutingProtocol _protocol;
  String _area;
  boolean _attach;
  boolean _down;
  IsisLevel _level;
  boolean _overload;
  String _systemId;

  @Override
  public IsisRoute build() {
    return new IsisRoute(
        getPrefixes(),
        getPrefixesBdd(),
        getNextHopIp(),
        getNextHopInterface(),
        getPreference(),
        getAdmin(),
        getTag(),
        getPath(),
        requireNonNull(_protocol),
        requireNonNull(_area),
        _attach,
        _down,
        requireNonNull(_level),
        _metric,
        _overload,
        requireNonNull(_systemId));
  }

  @Override
  protected IsisRouteBuilder getThis() {
    return this;
  }

  public RoutingProtocol getProtocol() {
    return _protocol;
  }

  public IsisRouteBuilder setProtocol(RoutingProtocol protocol) {
    _protocol = protocol;
    return this;
  }

  public IsisRouteBuilder setArea(String area) {
    _area = area;
    return this;
  }

  public IsisRouteBuilder setAttach(boolean attach) {
    _attach = attach;
    return this;
  }

  public IsisRouteBuilder setDown(boolean down) {
    _down = down;
    return this;
  }

  public IsisRouteBuilder setLevel(IsisLevel level) {
    _level = level;
    return this;
  }

  public IsisRouteBuilder setOverload(boolean overload) {
    _overload = overload;
    return this;
  }

  public IsisRouteBuilder setSystemId(String systemId) {
    _systemId = systemId;
    return this;
  }

  @Override
  public int getPreference() {
    return _preference == Attributes.UNSET_ROUTE_PREFERENCE
        ? (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(_protocol))
        : _preference;
  }

  @Override
  public int getAdmin() {
    return _admin == Attributes.UNSET_ROUTE_ADMIN
        ? (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(_protocol))
        : _admin;
  }
}
