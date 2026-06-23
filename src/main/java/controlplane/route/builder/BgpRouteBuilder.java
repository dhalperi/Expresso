package controlplane.route.builder;

import controlplane.route.Attributes;
import controlplane.route.BgpRoute;
import controlplane.route.RoutingProtocol;
import datamodel.aspath.AsPathIntf;
import datamodel.community.CommunityListFactory;
import datamodel.community.CommunityListIntf;
import main.Configuration;

public final class BgpRouteBuilder extends DynamicRouteBuilder<BgpRouteBuilder, BgpRoute> {
  int _weight = Attributes.BGP_ROUTE_DEFAULT_WEIGHT;
  int _localPreference = Attributes.BGP_ROUTE_DEFAULT_LOCAL_PREFERENCE;
  AsPathIntf _asPath = Attributes.BGP_ROUTE_DEFAULT_AS_PATH;
  long _med = Attributes.BGP_ROUTE_DEFAULT_MED;
  BgpRoute.OriginType _origin = Attributes.BGP_ROUTE_DEFAULT_ORIGIN;
  CommunityListIntf _communityList = CommunityListFactory.empty();
  BgpRoute.BgpRouteType _bgpRouteType = Attributes.BGP_ROUTE_DEFAULT_ROUTE_TYPE;
  long _igpCostToNextHop = Attributes.BGP_ROUTE_DEFAULT_IGP_COST_TO_NEXT_HOP;
  boolean _suppressed = false;

  @Override
  protected BgpRouteBuilder getThis() {
    return this;
  }

  @Override
  public BgpRoute build() {
    return new BgpRoute(
        getPrefixes(),
        getPrefixesBdd(),
        getNextHopIp(),
        getNextHopInterface(),
        getPreference(),
        getAdmin(),
        getTag(),
        getPath(),
        _weight,
        _localPreference,
        _asPath,
        _med,
        _origin,
        _communityList,
        _bgpRouteType,
        _igpCostToNextHop,
        _suppressed);
  }

  public BgpRouteBuilder setWeight(int weight) {
    _weight = weight;
    return this;
  }

  public BgpRouteBuilder setLocalPreference(int localPreference) {
    _localPreference = localPreference;
    return this;
  }

  public BgpRouteBuilder setAsPath(AsPathIntf asPath) {
    _asPath = asPath;
    return this;
  }

  public BgpRouteBuilder setMed(long med) {
    _med = med;
    return this;
  }

  public BgpRouteBuilder setOrigin(BgpRoute.OriginType origin) {
    _origin = origin;
    return this;
  }

  public BgpRouteBuilder setCommunityList(CommunityListIntf communityList) {
    _communityList = communityList;
    return this;
  }

  public BgpRouteBuilder setType(BgpRoute.BgpRouteType bgpRouteType) {
    _bgpRouteType = bgpRouteType;
    return this;
  }

  public BgpRouteBuilder setIgpCostToNextHop(long igpCostToNextHop) {
    _igpCostToNextHop = igpCostToNextHop;
    return this;
  }

  public BgpRouteBuilder setSuppressed(boolean suppressed) {
    _suppressed = suppressed;
    return this;
  }

  @Override
  public int getPreference() {
    RoutingProtocol rp =
        _bgpRouteType == BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR
            ? RoutingProtocol.EBGP
            : RoutingProtocol.IBGP;
    return _preference == Attributes.UNSET_ROUTE_PREFERENCE
        ? (_preference = Configuration.DEFAULT_VENDOR.getDefaultExternalPreference(rp))
        : _preference;
  }

  @Override
  public int getAdmin() {
    RoutingProtocol rp =
        _bgpRouteType == BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR
            ? RoutingProtocol.EBGP
            : RoutingProtocol.IBGP;
    return _admin == Attributes.UNSET_ROUTE_ADMIN
        ? (_admin = Configuration.DEFAULT_VENDOR.getDefaultInternalPreference(rp))
        : _admin;
  }

  public int getWeight() {
    return _weight;
  }

  public int getLocalPreference() {
    return _localPreference;
  }

  public AsPathIntf getAsPath() {
    return _asPath;
  }

  public long getMed() {
    return _med;
  }

  public BgpRoute.OriginType getOrigin() {
    return _origin;
  }

  public BgpRoute.BgpRouteType getBgpRouteType() {
    return _bgpRouteType;
  }

  public long getIgpCostToNextHop() {
    return _igpCostToNextHop;
  }
}
