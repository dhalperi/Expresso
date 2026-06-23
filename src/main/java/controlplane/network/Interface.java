package controlplane.network;

import controlplane.process.ospf.OspfIntfSetting;
import datamodel.ipv4.Ip;
import datamodel.ipv4.IpAddress;
import datamodel.path.PathHop;
import datamodel.acl.Acl;
import datamodel.trafficpolicy.TrafficPolicy;
import org.batfish.datamodel.isis.IsisInterfaceSettings;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Interface implements PathHop {

  public enum InterfaceType {
    AGGREGATED,
    AGGREGATE_CHILD,
    LOGICAL,
    LOOPBACK,
    NULL,
    PHYSICAL,
    REDUNDANT,
    TUNNEL,
    UNKNOWN,
    VLAN,
    VPN,
    NVE
  }

  Router router;
  VirtualRouter vrf;

  InterfaceName interfaceName;
  InterfaceType type;

  IpAddress primaryIpAddress;
  List<IpAddress> secondaryIpAddresses;

  boolean active;
  String channelGroup;

  /** The nominal bandwidth of this interface in bits/sec for use in protocol cost calculations. */
  double bandwidth;

  OspfIntfSetting ospfIntfSetting;
  IsisInterfaceSettings isisIntfSettings;

  Acl inboundAcl;
  Acl outboundAcl;

  // PBR
  TrafficPolicy inboundTrafficPolicy;
  TrafficPolicy outboundTrafficPolicy;

  private Interface(
      Router router,
      InterfaceName interfaceName,
      InterfaceType type,
      IpAddress primaryIpAddress,
      List<IpAddress> secondaryIpAddresses,
      boolean active,
      String channelGroup,
      double bandwidth,
      OspfIntfSetting ospfIntfSetting,
      IsisInterfaceSettings isisIntfSettings,
      Acl inboundAcl,
      Acl outboundAcl,
      TrafficPolicy inboundTrafficPolicy,
      TrafficPolicy outboundTrafficPolicy) {
    this.router = router;

    this.interfaceName = interfaceName;
    this.type = type;

    this.primaryIpAddress = primaryIpAddress;
    this.secondaryIpAddresses = secondaryIpAddresses;

    this.active = active;
    this.channelGroup = channelGroup;

    this.bandwidth = bandwidth;
    this.ospfIntfSetting = ospfIntfSetting;
    this.isisIntfSettings = isisIntfSettings;

    this.inboundAcl = inboundAcl;
    this.outboundAcl = outboundAcl;

    this.inboundTrafficPolicy = inboundTrafficPolicy;
    this.outboundTrafficPolicy = outboundTrafficPolicy;
  }

  public boolean isLoopback() {
    return type == InterfaceType.LOOPBACK;
  }

  public boolean isBlackhole() {
    return type == InterfaceType.NULL;
  }

  public Router getRouter() {
    return router;
  }

  public void setVrf(VirtualRouter vrf) {
    this.vrf = vrf;
  }

  public VirtualRouter getVrf() {
    return vrf;
  }

  public InterfaceName getInterfaceName() {
    return interfaceName;
  }

  public InterfaceType getType() {
    return type;
  }

  /**
   * helper function.
   *
   * @return 1. null if the primary ip address of this interface is null <br>
   *     2. else the ip of the primary ip address of this interface
   */
  @Nullable
  public Ip getIp() {
    return primaryIpAddress == null ? null : primaryIpAddress.getIp();
  }

  @Nullable
  public IpAddress getPrimaryIpAddress() {
    return primaryIpAddress;
  }

  public List<IpAddress> getSecondaryIpAddresses() {
    return secondaryIpAddresses;
  }

  public List<IpAddress> getAllIpAddresses() {
    List<IpAddress> all = new ArrayList<>();
    if (primaryIpAddress != null) all.add(primaryIpAddress);
    if (secondaryIpAddresses != null) all.addAll(secondaryIpAddresses);
    return all;
  }

  public boolean isActive() {
    return active;
  }

  public String getChannelGroup() {
    return channelGroup;
  }

  public double getBandwidth() {
    return bandwidth;
  }

  public OspfIntfSetting getOspfIntfSetting() {
    return ospfIntfSetting;
  }

  public IsisInterfaceSettings getIsisIntfSettings() {
    return isisIntfSettings;
  }

  public Acl getInboundAcl() {
    return inboundAcl;
  }

  public Acl getOutboundAcl() {
    return outboundAcl;
  }

  public TrafficPolicy getInboundTrafficPolicy() {
    return inboundTrafficPolicy;
  }

  public TrafficPolicy getOutboundTrafficPolicy() {
    return outboundTrafficPolicy;
  }

  @Override
  public String toString() {
    return interfaceName.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Interface anInterface = (Interface) o;
    return Objects.equals(interfaceName, anInterface.interfaceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(interfaceName);
  }

  @Override
  public String hopString() {
    return interfaceName.toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Router router;
    InterfaceName interfaceName;
    InterfaceType type;
    IpAddress primaryIpAddress;
    List<IpAddress> secondaryIpAddresses;
    boolean active;
    String channelGroup;
    double bandwidth;
    OspfIntfSetting ospfIntfSetting;
    IsisInterfaceSettings isisIntfSettings;
    Acl inboundAcl = Acl.PERMIT_ALL;
    Acl outboundAcl = Acl.PERMIT_ALL;
    TrafficPolicy inboundTrafficPolicy;
    TrafficPolicy outboundTrafficPolicy;

    private Builder() {}

    public Interface build() {
      return new Interface(
          router,
          interfaceName,
          type,
          primaryIpAddress,
          secondaryIpAddresses,
          active,
          channelGroup,
          bandwidth,
          ospfIntfSetting,
          isisIntfSettings,
          inboundAcl,
          outboundAcl,
          inboundTrafficPolicy,
          outboundTrafficPolicy);
    }

    public Builder setRouter(Router router) {
      this.router = router;
      return this;
    }

    public Builder setInterfaceName(InterfaceName interfaceName) {
      this.interfaceName = interfaceName;
      return this;
    }

    public Builder setType(InterfaceType type) {
      this.type = type;
      return this;
    }

    public Builder setPrimaryIpAddress(IpAddress primaryIpAddress) {
      this.primaryIpAddress = primaryIpAddress;
      return this;
    }

    public Builder setSecondaryIpAddresses(List<IpAddress> secondaryIpAddresses) {
      this.secondaryIpAddresses = secondaryIpAddresses;
      return this;
    }

    public Builder setActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder setChannelGroup(String channelGroup) {
      this.channelGroup = channelGroup;
      return this;
    }

    public Builder setBandwidth(double bandwidth) {
      this.bandwidth = bandwidth;
      return this;
    }

    public Builder setOspfIntfSetting(OspfIntfSetting ospfIntfSetting) {
      this.ospfIntfSetting = ospfIntfSetting;
      return this;
    }

    public Builder setIsisIntfSetting(IsisInterfaceSettings isisIntfSettings) {
      this.isisIntfSettings = isisIntfSettings;
      return this;
    }

    public Builder setInboundAcl(Acl inboundAcl) {
      this.inboundAcl = inboundAcl;
      return this;
    }

    public Builder setOutboundAcl(Acl outboundAcl) {
      this.outboundAcl = outboundAcl;
      return this;
    }

    public Builder setInboundTrafficPolicy(TrafficPolicy inboundTrafficPolicy) {
      this.inboundTrafficPolicy = inboundTrafficPolicy;
      return this;
    }

    public Builder setOutboundTrafficPolicy(TrafficPolicy outboundTrafficPolicy) {
      this.outboundTrafficPolicy = outboundTrafficPolicy;
      return this;
    }
  }
}
