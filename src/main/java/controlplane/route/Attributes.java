package controlplane.route;

import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import datamodel.aspath.AsPathFactory;
import datamodel.aspath.AsPathIntf;
import datamodel.community.CommunityListIntf;
import datamodel.ipv4.Ip;
import datamodel.path.Path;
import org.batfish.datamodel.isis.IsisLevel;

import java.util.Objects;

/** This class is used to identify different set of routes (i.e., route equivalence classes). */
public class Attributes {
  public static final int UNSET_ROUTE_PREFIXES_BDD = -1;
  public static final Ip UNSET_ROUTE_NEXT_HOP_IP = Ip.AUTO;
  public static final Interface UNSET_NEXT_HOP_INTERFACE = null;
  public static final int UNSET_ROUTE_PREFERENCE = Integer.MAX_VALUE;
  public static final int UNSET_ROUTE_ADMIN = Integer.MAX_VALUE;
  public static final long UNSET_ROUTE_TAG = -1L;
  public static final long UNSET_ROUTE_METRIC = Long.MAX_VALUE;

  // todo check these bgp route default values
  public static final int BGP_ROUTE_DEFAULT_WEIGHT = 0;
  public static final int BGP_ROUTE_DEFAULT_LOCAL_PREFERENCE = 100;
  public static final AsPathIntf BGP_ROUTE_DEFAULT_AS_PATH = AsPathFactory.empty();
  public static final long BGP_ROUTE_DEFAULT_MED = 0;
  public static final BgpRoute.OriginType BGP_ROUTE_DEFAULT_ORIGIN = BgpRoute.OriginType.IGP;
  public static final BgpRoute.BgpRouteType BGP_ROUTE_DEFAULT_ROUTE_TYPE =
      BgpRoute.BgpRouteType.NETWORK;

  public static final long BGP_ROUTE_DEFAULT_IGP_COST_TO_NEXT_HOP = Long.MAX_VALUE;

  public static String printTag(long tag) {
    return tag == UNSET_ROUTE_TAG ? "NO_TAG" : String.valueOf(tag);
  }

  public static String printIgpCostToNextHop(long igpCostToNextHop) {
    return igpCostToNextHop == BGP_ROUTE_DEFAULT_IGP_COST_TO_NEXT_HOP
        ? "NEXT_HOP_UNREACHABLE"
        : String.valueOf(igpCostToNextHop);
  }

  // basic attributes
  public final Ip nextHopIp;
  public final Interface nextHopInterface;
  public final Integer preference;
  public final Integer admin;
  public final Long tag;

  public final Path path;

  // ospf attributes
  public final Long cost;

  // isis attributes
  public final RoutingProtocol routingProtocol;
  public final String area;
  public final Boolean attach;
  public final Boolean down;
  public final IsisLevel level;
  public final Long metric;
  public final Boolean overload;
  public final String systemId;

  // bgp attributes
  public final Integer weight;
  public final Integer localPreference;
  public final AsPathIntf asPath;
  public final Long med;
  public final BgpRoute.OriginType origin;
  public final CommunityListIntf communityList;
  public final BgpRoute.BgpRouteType bgpRouteType;
  public final Long igpCostToNextHop;
  public final Boolean suppressed;

  public Attributes(
      Ip nextHopIp,
      Interface nextHopInterface,
      Integer preference,
      Integer admin,
      Long tag,
      Path path,
      Long cost,
      RoutingProtocol routingProtocol,
      String area,
      Boolean attach,
      Boolean down,
      IsisLevel level,
      Long metric,
      Boolean overload,
      String systemId,
      Integer weight,
      Integer localPreference,
      AsPathIntf asPath,
      Long med,
      BgpRoute.OriginType origin,
      CommunityListIntf communityList,
      BgpRoute.BgpRouteType bgpRouteType,
      Long igpCostToNextHop,
      Boolean suppressed) {
    this.nextHopIp = nextHopIp;
    this.nextHopInterface = nextHopInterface;
    this.preference = preference;
    this.admin = admin;
    this.tag = tag;
    this.path = path;
    this.cost = cost;
    this.routingProtocol = routingProtocol;
    this.area = area;
    this.attach = attach;
    this.down = down;
    this.level = level;
    this.metric = metric;
    this.overload = overload;
    this.systemId = systemId;
    this.weight = weight;
    this.localPreference = localPreference;
    this.asPath = asPath;
    this.med = med;
    this.origin = origin;
    this.communityList = communityList;
    this.bgpRouteType = bgpRouteType;
    this.igpCostToNextHop = igpCostToNextHop;
    this.suppressed = suppressed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Attributes that = (Attributes) o;
    return Objects.equals(nextHopIp, that.nextHopIp)
        && Objects.equals(nextHopInterface, that.nextHopInterface)
        && Objects.equals(preference, that.preference)
        && Objects.equals(admin, that.admin)
        && Objects.equals(tag, that.tag)
        && Objects.equals(path, that.path)
        && Objects.equals(cost, that.cost)
        && Objects.equals(routingProtocol, that.routingProtocol)
        && Objects.equals(area, that.area)
        && Objects.equals(attach, that.attach)
        && Objects.equals(down, that.down)
        && Objects.equals(level, that.level)
        && Objects.equals(metric, that.metric)
        && Objects.equals(overload, that.overload)
        && Objects.equals(systemId, that.systemId)
        && Objects.equals(weight, that.weight)
        && Objects.equals(localPreference, that.localPreference)
        && Objects.equals(asPath, that.asPath)
        && Objects.equals(med, that.med)
        && Objects.equals(origin, that.origin)
        && Objects.equals(communityList, that.communityList)
        && Objects.equals(bgpRouteType, that.bgpRouteType)
        && Objects.equals(igpCostToNextHop, that.igpCostToNextHop)
        && Objects.equals(suppressed, that.suppressed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        nextHopIp,
        nextHopInterface,
        preference,
        admin,
        tag,
        path,
        cost,
        routingProtocol,
        area,
        attach,
        down,
        level,
        metric,
        overload,
        systemId,
        weight,
        localPreference,
        asPath,
        med,
        origin,
        communityList,
        bgpRouteType,
        igpCostToNextHop,
        suppressed);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nextHopIp", nextHopIp)
        .add("nextHopInterface", nextHopInterface)
        .add("preference", preference)
        .add("admin", admin)
        .add("tag", tag)
        .add("path", path)
        .add("cost(OSPF)", cost)
        .add("routingProtocol(ISIS)", routingProtocol)
        .add("area(ISIS)", area)
        .add("attach(ISIS)", attach)
        .add("down(ISIS)", down)
        .add("level(ISIS)", level)
        .add("metric(ISIS)", metric)
        .add("overload(ISIS)", overload)
        .add("systemId(ISIS)", systemId)
        .add("weight(BGP)", weight)
        .add("localPreference(BGP)", localPreference)
        .add("asPath(BGP)", asPath)
        .add("med(BGP)", med)
        .add("origin(BGP)", origin)
        .add("communities(BGP)", communityList)
        .add("bgpRouteType(BGP)", bgpRouteType)
        .add("igpCostToNextHop(BGP)", igpCostToNextHop)
        .add("suppressed(BGP)", suppressed)
        .omitNullValues()
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getPreference() {
    return preference;
  }

  public static class Builder {
    // basic attributes
    Ip nextHopIp;
    Interface nextHopInterface;
    Integer preference;
    Integer admin;
    Long tag;

    Path path;

    // ospf attributes
    Long cost;

    // isis attributes
    RoutingProtocol routingProtocol;
    String area;
    Boolean attach;
    Boolean down;
    IsisLevel level;
    Long metric;
    Boolean overload;
    String systemId;

    // bgp attributes
    Integer weight;
    Integer localPreference;
    AsPathIntf asPath;
    Long med;
    BgpRoute.OriginType origin;
    CommunityListIntf communityList;
    BgpRoute.BgpRouteType bgpRouteType;
    Long igpCostToNextHop;
    Boolean suppressed;

    private Builder() {}

    public Attributes build() {
      return new Attributes(
          nextHopIp,
          nextHopInterface,
          preference,
          admin,
          tag,
          path,
          cost,
          routingProtocol,
          area,
          attach,
          down,
          level,
          metric,
          overload,
          systemId,
          weight,
          localPreference,
          asPath,
          med,
          origin,
          communityList,
          bgpRouteType,
          igpCostToNextHop,
          suppressed);
    }

    public Builder setNextHopIp(Ip nextHopIp) {
      this.nextHopIp = nextHopIp;
      return this;
    }

    public Builder setNextHopInterface(Interface nextHopInterface) {
      this.nextHopInterface = nextHopInterface;
      return this;
    }

    public Builder setPreference(int preference) {
      this.preference = preference;
      return this;
    }

    public Builder setAdmin(int admin) {
      this.admin = admin;
      return this;
    }

    public Builder setTag(long tag) {
      this.tag = tag;
      return this;
    }

    public Builder setPath(Path path) {
      this.path = path;
      return this;
    }

    public Builder setCost(long cost) {
      this.cost = cost;
      return this;
    }

    public Builder setRoutingProtocol(RoutingProtocol routingProtocol) {
      this.routingProtocol = routingProtocol;
      return this;
    }

    public Builder setArea(String area) {
      this.area = area;
      return this;
    }

    public Builder setAttach(Boolean attach) {
      this.attach = attach;
      return this;
    }

    public Builder setDown(Boolean down) {
      this.down = down;
      return this;
    }

    public Builder setLevel(IsisLevel level) {
      this.level = level;
      return this;
    }

    public Builder setMetric(Long metric) {
      this.metric = metric;
      return this;
    }

    public Builder setOverload(Boolean overload) {
      this.overload = overload;
      return this;
    }

    public Builder setSystemId(String systemId) {
      this.systemId = systemId;
      return this;
    }

    public Builder setWeight(int weight) {
      this.weight = weight;
      return this;
    }

    public Builder setLocalPreference(int localPreference) {
      this.localPreference = localPreference;
      return this;
    }

    public Builder setAsPath(AsPathIntf asPath) {
      this.asPath = asPath;
      return this;
    }

    public Builder setMed(long med) {
      this.med = med;
      return this;
    }

    public Builder setOrigin(BgpRoute.OriginType origin) {
      this.origin = origin;
      return this;
    }

    public Builder setCommunityList(CommunityListIntf communityList) {
      this.communityList = communityList;
      return this;
    }

    public Builder setBgpRouteType(BgpRoute.BgpRouteType bgpRouteType) {
      this.bgpRouteType = bgpRouteType;
      return this;
    }

    public Builder setIgpCostToNextHop(long igpCostToNextHop) {
      this.igpCostToNextHop = igpCostToNextHop;
      return this;
    }

    public Builder setSuppressed(boolean suppressed) {
      this.suppressed = suppressed;
      return this;
    }
  }
}
