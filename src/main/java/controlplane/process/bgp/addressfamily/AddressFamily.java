package controlplane.process.bgp.addressfamily;

import com.google.common.base.MoreObjects;
import datamodel.filterlist.FilterList;
import datamodel.routepolicy.RoutePolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;

/** Base class for all BGP address family config */
@ParametersAreNonnullByDefault
public abstract class AddressFamily implements Serializable {

  @Nonnull AddressFamilyCapabilities addressFamilyCapabilities;

  /**
   * If this is true, a BGP device will exchange routes with a specified peer or peer group in the
   * address family view. By default, only the peer in the BGP IPv4 unicast address family view is
   * automatically enabled. <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1100127035/fc1ac4f2/peer-enable-bgp">huawei-doc</a>
   */
  boolean enable;

  boolean routeReflectorClient;

  /**
   * If this is true, a BGP device will set its IP address as the next hop of routes when it
   * advertises routes to an IBGP peer or peer group. <br>
   * This option is usually set on an ASBR. By default, when an ASBR forwards a route learned from
   * an EBGP peer to its IBGP peers, the ASBR does not change the next hop of the route. The next
   * hop address of a route advertised by an EBGP peer is the address of the EBGP peer. After being
   * forwarded to the IBGP peers, the route cannot become an active route because of the unreachable
   * next hop. <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1100064351/ff6c588/peer-next-hop-local">huawei-doc</a>
   */
  boolean nextHopLocal;

  /**
   * If this is true, the AS-path of a BGP route cannot carry private AS numbers. By default, the
   * AS-path of a BGP route is allowed to carry private AS numbers. <br>
   * However, there are two cases in which the private AS numbers are not deleted even if this is
   * true. If we want to enable the device to delete the private AS numbers unconditionally, we have
   * to specify "force". <br>
   * Currently, we do not support the "force" option of the "public-as-only" command. <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1100008283/d86b50cb/peer-public-as-only">huawei-doc</a>
   */
  boolean publicAsOnly;
  /**
   * It this is true, a BGP device sends a default route with the next hop as itself to its peer or
   * peer group regardless of whether default routes exist in the routing table. <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1100197677/3e4bff5e/peer-default-route-advertise">huawei-doc</a>
   */
  boolean defaultRouteAdvertise;

  /**
   * If this is true, the local AS number can be repeated. We can specify the number of local AS
   * number repetitions. <br>
   * <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1100008283/14418f9d/peer-allow-as-loop">huawei-doc</a>
   * Generally, BGP checks the AS path attribute of each route received from a peer. If the local AS
   * number is carried in a route, BGP discard this route to prevent routing loops. <br>
   * However, we can use this option in some special applications to allow the AS path attribute of
   * a route received from a peer to contain the local AS number. It limits the number of
   * repetitions for the local AS number.
   */
  int allowAsLoop;

  /**
   * An integer that ranges from 0 to 65535. On Cisco devices, it's called weight. On Huawei
   * devices, it's called preferred value. After a preferred value is set to a BGP peer, all the
   * routes learned from the specified peer have the preferred value. If there are multiple routes
   * to the same address prefix, the route with the highest preferred value is preferred. By
   * default, the preferred value of a route learned from a BGP peer is 0.
   */
  int weight;

  @Nullable FilterList importPrefixFilter;
  @Nullable FilterList importCommunityFilter;
  @Nullable FilterList importAsPathFilter;
  @Nullable RoutePolicy importRoutePolicy;
  @Nullable FilterList exportPrefixFilter;
  @Nullable FilterList exportCommunityFilter;
  @Nullable FilterList exportAsPathFilter;
  @Nullable RoutePolicy exportRoutePolicy;

  public AddressFamily(
      AddressFamilyCapabilities addressFamilyCapabilities,
      boolean enable,
      boolean routeReflectorClient,
      boolean nextHopLocal,
      boolean publicAsOnly,
      boolean defaultRouteAdvertise,
      int allowAsLoop,
      int weight,
      @Nullable FilterList importPrefixFilter,
      @Nullable FilterList importCommunityFilter,
      @Nullable FilterList importAsPathFilter,
      @Nullable RoutePolicy importRoutePolicy,
      @Nullable FilterList exportPrefixFilter,
      @Nullable FilterList exportCommunityFilter,
      @Nullable FilterList exportAsPathFilter,
      @Nullable RoutePolicy exportRoutePolicy) {
    this.addressFamilyCapabilities = addressFamilyCapabilities;
    this.enable = enable;
    this.routeReflectorClient = routeReflectorClient;
    this.nextHopLocal = nextHopLocal;
    this.publicAsOnly = publicAsOnly;
    this.defaultRouteAdvertise = defaultRouteAdvertise;
    this.allowAsLoop = allowAsLoop;
    this.weight = weight;
    this.importPrefixFilter = importPrefixFilter;
    this.importCommunityFilter = importCommunityFilter;
    this.importAsPathFilter = importAsPathFilter;
    this.importRoutePolicy = importRoutePolicy;
    this.exportPrefixFilter = exportPrefixFilter;
    this.exportCommunityFilter = exportCommunityFilter;
    this.exportAsPathFilter = exportAsPathFilter;
    this.exportRoutePolicy = exportRoutePolicy;
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(@Nullable Object obj);

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("addressFamilyCapabilities", addressFamilyCapabilities)
        .add("enable", enable)
        .add("routeReflectorClient", routeReflectorClient)
        .add("nextHopLocal", nextHopLocal)
        .add("publicAsOnly", publicAsOnly)
        .add("defaultRouteAdvertise", defaultRouteAdvertise)
        .add("allowAsLoop", allowAsLoop)
        .add("weight", weight)
        .add("importPrefixFilter", importPrefixFilter == null ? null : importPrefixFilter.getName())
        .add(
            "importCommunityFilter",
            importCommunityFilter == null ? null : importCommunityFilter.getName())
        .add("importAsPathFilter", importAsPathFilter == null ? null : importAsPathFilter.getName())
        .add("importRoutePolicy", importRoutePolicy == null ? null : importRoutePolicy)
        .add("exportPrefixFilter", exportPrefixFilter == null ? null : exportPrefixFilter.getName())
        .add(
            "exportCommunityFilter",
            exportCommunityFilter == null ? null : exportCommunityFilter.getName())
        .add("exportAsPathFilter", exportAsPathFilter == null ? null : exportAsPathFilter.getName())
        .add("exportRoutePolicy", exportRoutePolicy == null ? null : exportRoutePolicy)
        .toString();
  }

  public abstract AddressFamily.Type getType();

  @Nonnull
  public AddressFamilyCapabilities getAddressFamilyCapabilities() {
    return addressFamilyCapabilities;
  }

  public boolean isEnable() {
    return enable;
  }

  public boolean isRouteReflectorClient() {
    return routeReflectorClient;
  }

  public int getAllowAsLoop() {
    return allowAsLoop;
  }

  public boolean isDefaultRouteAdvertise() {
    return defaultRouteAdvertise;
  }

  @Nullable
  public FilterList getImportPrefixFilter() {
    return importPrefixFilter;
  }

  @Nullable
  public RoutePolicy getImportRoutePolicy() {
    return importRoutePolicy;
  }

  @Nullable
  public FilterList getExportPrefixFilter() {
    return exportPrefixFilter;
  }

  @Nullable
  public RoutePolicy getExportRoutePolicy() {
    return exportRoutePolicy;
  }

  /** Builder for an {@link AddressFamily} */
  @ParametersAreNonnullByDefault
  public abstract static class Builder<B extends Builder<B, F>, F extends AddressFamily> {

    // todo check their default values
    @Nullable AddressFamilyCapabilities addressFamilyCapabilities;
    boolean enable = false;
    boolean routeReflectorClient = false;
    boolean nextHopLocal = false;
    boolean publicAsOnly = false;
    boolean defaultRouteAdvertise = false;
    int allowAsLoop = 0;
    int weight = 0;
    @Nullable FilterList importPrefixFilter;
    @Nullable FilterList importCommunityFilter;
    @Nullable FilterList importAsPathFilter;
    @Nullable RoutePolicy importRoutePolicy;
    @Nullable FilterList exportPrefixFilter;
    @Nullable FilterList exportCommunityFilter;
    @Nullable FilterList exportAsPathFilter;
    @Nullable RoutePolicy exportRoutePolicy;

    public B setAddressFamilyCapabilities(
        @Nonnull AddressFamilyCapabilities addressFamilyCapabilities) {
      this.addressFamilyCapabilities = addressFamilyCapabilities;
      return getThis();
    }

    public B setEnable(boolean enable) {
      this.enable = enable;
      return getThis();
    }

    public B setRouteReflectorClient(boolean routeReflectorClient) {
      this.routeReflectorClient = routeReflectorClient;
      return getThis();
    }

    public B setNextHopLocal(boolean nextHopLocal) {
      this.nextHopLocal = nextHopLocal;
      return getThis();
    }

    public B setPublicAsOnly(boolean publicAsOnly) {
      this.publicAsOnly = publicAsOnly;
      return getThis();
    }

    public B setDefaultRouteAdvertise(boolean defaultRouteAdvertise) {
      this.defaultRouteAdvertise = defaultRouteAdvertise;
      return getThis();
    }

    public B setAllowAsLoop(int allowAsLoop) {
      this.allowAsLoop = allowAsLoop;
      return getThis();
    }

    public B setWeight(int weight) {
      this.weight = weight;
      return getThis();
    }

    public B setImportPrefixFilter(@Nullable FilterList importPrefixFilter) {
      this.importPrefixFilter = importPrefixFilter;
      return getThis();
    }

    public B setImportCommunityFilter(@Nullable FilterList importCommunityFilter) {
      this.importCommunityFilter = importCommunityFilter;
      return getThis();
    }

    public B setImportAsPathFilter(@Nullable FilterList importAsPathFilter) {
      this.importAsPathFilter = importAsPathFilter;
      return getThis();
    }

    public B setImportRoutePolicy(@Nullable RoutePolicy importRoutePolicy) {
      this.importRoutePolicy = importRoutePolicy;
      return getThis();
    }

    public B setExportPrefixFilter(@Nullable FilterList exportPrefixFilter) {
      this.exportPrefixFilter = exportPrefixFilter;
      return getThis();
    }

    public B setExportCommunityFilter(@Nullable FilterList exportCommunityFilter) {
      this.exportCommunityFilter = exportCommunityFilter;
      return getThis();
    }

    public B setExportAsPathFilter(@Nullable FilterList exportAsPathFilter) {
      this.exportAsPathFilter = exportAsPathFilter;
      return getThis();
    }

    public B setExportRoutePolicy(@Nullable RoutePolicy exportRoutePolicy) {
      this.exportRoutePolicy = exportRoutePolicy;
      return getThis();
    }

    @Nonnull
    public abstract B getThis();

    @Nonnull
    public abstract F build();
  }

  /** BGP address family type */
  public enum Type {
    IPV4_UNICAST,
    EVPN
  }
}
