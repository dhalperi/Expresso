package controlplane.process.bgp;

import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import controlplane.process.bgp.addressfamily.Ipv4UnicastAddressFamily;
import controlplane.route.BgpRoute;
import datamodel.ipv4.Ip;
import datamodel.route.RouteFilterResult;

import javax.annotation.Nullable;

/**
 * There are three kinds of {@link org.batfish.datamodel.BgpPeerConfig} in batfish. {@link
 * org.batfish.datamodel.BgpActivePeerConfig} {@link org.batfish.datamodel.BgpPassivePeerConfig}
 * {@link org.batfish.datamodel.BgpUnnumberedPeerConfig}
 */
public class BgpPeerConfig {
  BgpRoutingProcess process;

  Ip localIp;
  final Ip remoteIp;
  final long localAS;
  final long remoteAS;
  @Nullable final Interface connectInterface;
  final String description;
  @Nullable final String group;
  /**
   * <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1000041693/e1a62bc6/peer-ignore">huawei-doc</a>
   * The peer ignore command prevents a BGP device from establishing a session with a peer or peer
   * group.
   */
  final boolean ignore;

  final boolean internal;
  final boolean external;
  final int ebgpMaxHop;
  final Ipv4UnicastAddressFamily ipv4UnicastAddressFamily;

  /**
   * The remote bgp process is a {@link PeerType} peer of the local bgp process. <br>
   * E.g., clients of a RR is typed {@link PeerType#CLIENT} and routes sending to its clients are
   * typed {@link controlplane.route.BgpRoute.BgpRouteType#FROM_ROUTE_REFLECTOR}
   */
  PeerType peerType;

  public enum PeerType {
    EBGP(BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR),
    IBGP(BgpRoute.BgpRouteType.FROM_IBGP_NEIGHBOR),
    REFLECTOR(BgpRoute.BgpRouteType.FROM_CLIENT),
    CLIENT(BgpRoute.BgpRouteType.FROM_ROUTE_REFLECTOR);

    BgpRoute.BgpRouteType bgpRouteType;

    PeerType(BgpRoute.BgpRouteType bgpRouteType) {
      this.bgpRouteType = bgpRouteType;
    }
  }

  public BgpPeerConfig(
      Ip localIp,
      Ip remoteIp,
      long localAS,
      long remoteAS,
      @Nullable Interface connectInterface,
      String description,
      @Nullable String group,
      boolean ignore,
      boolean internal,
      boolean external,
      int ebgpMaxHop,
      Ipv4UnicastAddressFamily ipv4UnicastAddressFamily) {
    this.localIp = localIp;
    this.remoteIp = remoteIp;
    this.localAS = localAS;
    this.remoteAS = remoteAS;
    this.connectInterface = connectInterface;
    this.description = description;
    this.group = group;
    this.ignore = ignore;
    this.internal = internal;
    this.external = external;
    this.ebgpMaxHop = ebgpMaxHop;
    this.ipv4UnicastAddressFamily = ipv4UnicastAddressFamily;
  }

  public BgpRoutingProcess getProcess() {
    return process;
  }

  public Ip getLocalIp() {
    return localIp;
  }

  public Ip getRemoteIp() {
    return remoteIp;
  }

  public long getLocalAS() {
    return localAS;
  }

  public long getRemoteAS() {
    return remoteAS;
  }

  @Nullable
  public Interface getConnectInterface() {
    return connectInterface;
  }

  public String getDescription() {
    return description;
  }

  @Nullable
  public String getGroup() {
    return group;
  }

  public boolean isIgnore() {
    return ignore;
  }

  public Ipv4UnicastAddressFamily getIpv4UnicastAddressFamily() {
    return ipv4UnicastAddressFamily;
  }

  public void setProcess(BgpRoutingProcess process) {
    this.process = process;
  }

  public void setPeerType(PeerType peerType) {
    this.peerType = peerType;
  }

  /**
   * process the given bgp route.
   *
   * @param bgpRoute bgp route to be processed
   * @return {@link RouteFilterResult}.
   */
  public RouteFilterResult<BgpRoute> processIn(BgpRoute bgpRoute) {
    return ipv4UnicastAddressFamily.processIn(bgpRoute, this);
  }

  public RouteFilterResult<BgpRoute> processOut(BgpRoute bgpRoute) {
    return ipv4UnicastAddressFamily.processOut(bgpRoute, this);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("vrf", process.virtualRouter)
        .add("localIp", localIp)
        .add("remoteIp", remoteIp)
        .add("localAs", localAS)
        .add("remoteAs", remoteAS)
        .add("connectInterface", connectInterface)
        .add("description", description)
        .add("group", group)
        .add("routeReflectorClient", ipv4UnicastAddressFamily.isRouteReflectorClient())
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Ip localIp;
    Ip remoteIp;
    long localAS;
    long remoteAS;
    @Nullable Interface connectInterface;
    @Nullable String description;
    @Nullable String group;
    boolean ignore;
    boolean internal;
    boolean external;
    int ebgpMaxHop;
    Ipv4UnicastAddressFamily ipv4UnicastAddressFamily;

    private Builder() {}

    public BgpPeerConfig build() {
      return new BgpPeerConfig(
          localIp,
          remoteIp,
          localAS,
          remoteAS,
          connectInterface,
          description,
          group,
          ignore,
          internal,
          external,
          ebgpMaxHop,
          ipv4UnicastAddressFamily);
    }

    public Builder setLocalIp(Ip localIp) {
      this.localIp = localIp;
      return this;
    }

    public Builder setRemoteIp(Ip remoteIp) {
      this.remoteIp = remoteIp;
      return this;
    }

    public Builder setLocalAS(long localAS) {
      this.localAS = localAS;
      return this;
    }

    public Builder setRemoteAS(long remoteAS) {
      this.remoteAS = remoteAS;
      return this;
    }

    public Builder setConnectInterface(@Nullable Interface connectInterface) {
      this.connectInterface = connectInterface;
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = description;
      return this;
    }

    public Builder setGroup(@Nullable String group) {
      this.group = group;
      return this;
    }

    public Builder setIgnore(boolean ignore) {
      this.ignore = ignore;
      return this;
    }

    public Builder setInternal(boolean internal) {
      this.internal = internal;
      return this;
    }

    public Builder setExternal(boolean external) {
      this.external = external;
      return this;
    }

    public Builder setEbgpMaxHop(int ebgpMaxHop) {
      this.ebgpMaxHop = ebgpMaxHop;
      return this;
    }

    public Builder setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily ipv4UnicastAddressFamily) {
      this.ipv4UnicastAddressFamily = ipv4UnicastAddressFamily;
      return this;
    }
  }
}
