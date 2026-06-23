package controlplane.process.bgp.addressfamily;

import controlplane.process.bgp.BgpPeerConfig;
import controlplane.route.BgpRoute;
import datamodel.community.CommunityListFactory;
import datamodel.filterlist.FilterList;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.RoutePolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class Ipv4UnicastAddressFamily extends AddressFamily {

  public Ipv4UnicastAddressFamily(
      @Nonnull AddressFamilyCapabilities addressFamilyCapabilities,
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
    super(
        addressFamilyCapabilities,
        enable,
        routeReflectorClient,
        nextHopLocal,
        publicAsOnly,
        defaultRouteAdvertise,
        allowAsLoop,
        weight,
        importPrefixFilter,
        importCommunityFilter,
        importAsPathFilter,
        importRoutePolicy,
        exportPrefixFilter,
        exportCommunityFilter,
        exportAsPathFilter,
        exportRoutePolicy);
  }

  @Override
  public AddressFamily.Type getType() {
    return AddressFamily.Type.IPV4_UNICAST;
  }

  /**
   * Process the given bgp route through import filters and import route policy. <br>
   * Note that we put allow-as-loop filtering first as in Batfish (see
   * org.batfish.dataplane.ibdp.BgpRoutingProcess.transformBgpRouteOnExport() and
   * org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRoutePreExport()).
   *
   * @param bgpRoute bgp route to be processed
   * @return null if the route is denied, otherwise the processed route.
   */
  public RouteFilterResult<BgpRoute> processIn(BgpRoute bgpRoute, BgpPeerConfig localConfig) {
    // Set the route's weight to the preferred value specified for this peer.
    bgpRoute.setWeight(weight);
    RouteFilter[] filters =
        new RouteFilter[] {
          RoutePolicy.ALLOW_AS_LOOP,
          importPrefixFilter,
          importCommunityFilter,
          importAsPathFilter,
          importRoutePolicy
        };
    return process(bgpRoute, filters, true, localConfig);
  }

  /**
   * Process the given bgp route through export filters and export route policy. <br>
   * Note that we put remove-private-as first as in Batfish (E.g., see
   * org.batfish.representation.cisco_nxos.Conversions.getExportStatementsForIpv4()).
   *
   * @param bgpRoute bgp route to be processed
   * @return null if the route is denied, otherwise the processed route.
   */
  public RouteFilterResult<BgpRoute> processOut(BgpRoute bgpRoute, BgpPeerConfig localConfig) {
    RouteFilter[] filters =
        new RouteFilter[] {
          publicAsOnly ? RoutePolicy.REMOVE_PRIVATE_AS : RoutePolicy.PERMIT_ALL,
          exportPrefixFilter,
          exportCommunityFilter,
          exportAsPathFilter,
          exportRoutePolicy
        };
    RouteFilterResult<BgpRoute> result = process(bgpRoute, filters, false, localConfig);
    result
        .getPermitted()
        .getAllRoutes()
        .forEach(
            route -> {
              if (nextHopLocal) {
                route.setNextHopIp(localConfig.getLocalIp());
              }
              if (!addressFamilyCapabilities.isSendCommunity()) {
                route.setCommunities(CommunityListFactory.empty());
              }
            });
    return result;
  }

  /**
   * process the given bgp route through filters and route policy.
   *
   * @param bgpRoute bgp route to be processed
   * @return null if the route is denied, otherwise the processed route.
   */
  public RouteFilterResult<BgpRoute> process(
      BgpRoute bgpRoute, RouteFilter[] filters, boolean in, BgpPeerConfig localConfig) {
    RouteFilterResult<BgpRoute> result = new RouteFilterResult<>(bgpRoute);
    RouteSet<BgpRoute> remains = new RouteSet<>(bgpRoute);
    for (RouteFilter filter : filters) {
      if (filter != null) {
        RouteSet<BgpRoute> remainsTmp = new RouteSet<>();
        for (BgpRoute remain : remains.getAllRoutes()) {
          RouteFilterResult<BgpRoute> resultTmp =
              filter.filter(
                  remain,
                  new RouteFilterEnvironment.Builder<BgpRoute>()
                      .setIn(in)
                      .setLocalConfig(localConfig)
                      .build());
          result.getDenied().addAll(resultTmp.getDenied());
          remainsTmp.addAll(resultTmp.getPermitted());
        }
        remains = remainsTmp;
      }
    }
    result.getPermitted().addAll(remains);
    return result;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Ipv4UnicastAddressFamily)) {
      return false;
    }
    Ipv4UnicastAddressFamily that = (Ipv4UnicastAddressFamily) o;
    return addressFamilyCapabilities.equals(that.addressFamilyCapabilities)
        && enable == that.enable
        && routeReflectorClient == that.routeReflectorClient
        && nextHopLocal == that.nextHopLocal
        && publicAsOnly == that.publicAsOnly
        && defaultRouteAdvertise == that.defaultRouteAdvertise
        && allowAsLoop == that.allowAsLoop
        && weight == that.weight
        && Objects.equals(importPrefixFilter, that.importPrefixFilter)
        && Objects.equals(importCommunityFilter, that.importCommunityFilter)
        && Objects.equals(importAsPathFilter, that.importAsPathFilter)
        && Objects.equals(importRoutePolicy, that.importRoutePolicy)
        && Objects.equals(exportPrefixFilter, that.exportPrefixFilter)
        && Objects.equals(exportCommunityFilter, that.exportCommunityFilter)
        && Objects.equals(exportAsPathFilter, that.exportAsPathFilter)
        && Objects.equals(exportRoutePolicy, that.exportRoutePolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        addressFamilyCapabilities,
        enable,
        routeReflectorClient,
        nextHopLocal,
        publicAsOnly,
        defaultRouteAdvertise,
        allowAsLoop,
        weight,
        importPrefixFilter,
        importCommunityFilter,
        importAsPathFilter,
        importRoutePolicy,
        exportPrefixFilter,
        exportCommunityFilter,
        exportAsPathFilter,
        exportRoutePolicy);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder
      extends AddressFamily.Builder<Builder, Ipv4UnicastAddressFamily> {

    private Builder() {
      addressFamilyCapabilities = AddressFamilyCapabilities.builder().build();
      enable = true;
    }

    @Nonnull
    @Override
    public Builder getThis() {
      return this;
    }

    @Nonnull
    @Override
    public Ipv4UnicastAddressFamily build() {
      return new Ipv4UnicastAddressFamily(
          addressFamilyCapabilities,
          enable,
          routeReflectorClient,
          nextHopLocal,
          publicAsOnly,
          defaultRouteAdvertise,
          allowAsLoop,
          weight,
          importPrefixFilter,
          importCommunityFilter,
          importAsPathFilter,
          importRoutePolicy,
          exportPrefixFilter,
          exportCommunityFilter,
          exportAsPathFilter,
          exportRoutePolicy);
    }
  }
}
