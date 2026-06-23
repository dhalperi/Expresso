package inputparser.bfvi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.RedistributionConfig;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.bgp.addressfamily.AddressFamilyCapabilities;
import controlplane.process.bgp.addressfamily.Ipv4UnicastAddressFamily;
import controlplane.process.bgp.routeaggregation.RouteAggregation;
import controlplane.route.RoutingProtocol;
import datamodel.ipv4.Ip;
import datamodel.routepolicy.RoutePolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import static util.JsonUtil.getAsBooleanDefaultFalse;
import static util.JsonUtil.getAsIntOrDefault;
import static util.JsonUtil.getAsStringDefaultEmpty;
import static util.JsonUtil.getAsStringDefaultNull;

public class BgpParser {
  public static BgpRoutingProcess parseBgpProcess(
      VirtualRouter vrf,
      Vector<RouteAggregation> aggregations,
      Map<String, RoutePolicy> routeImports,
      JsonObject jsonBgp) {
    BgpRoutingProcess.Builder builder = BgpRoutingProcess.builder();

    HashSet<BgpPeerConfig> peers =
        parseBgpNeighbors(vrf.getRouter(), jsonBgp.getAsJsonObject("neighbors"));
    HashSet<RedistributionConfig> imports =
        peers.stream()
            .map(BgpPeerConfig::getRemoteIp)
            .flatMap(
                peerIp ->
                    routeImports.entrySet().stream()
                        .filter(e -> e.getKey().contains(peerIp.toString()))
                        .map(Map.Entry::getValue))
            .map(
                routePolicy ->
                    RedistributionConfig.builder()
                        .setVrf(vrf)
                        .setProtocol(RoutingProtocol.STATIC)
                        .setRoutePolicy(routePolicy)
                        .build())
            .collect(Collectors.toCollection(HashSet::new));

    builder
        .setVirtualRouter(vrf)
        .setRouterId(Ip.of(getAsStringDefaultNull(jsonBgp, "routerId")))
        .setNeighbors(peers)
        .setNetworks(new HashMap<>())
        .setAggregations(aggregations)
        .setPreference(new int[] {255, 255, 255})
        .setDefaultLocalPreference(100)
        .setMaxLoadBalancingIbgp(getAsBooleanDefaultFalse(jsonBgp, "multipathIbgp") ? 1 : 100)
        .setMaxLoadBalancingEbgp(getAsBooleanDefaultFalse(jsonBgp, "multipathEbgp") ? 1 : 100)
        .setImports(imports);

    return builder.build();
  }

  public static HashSet<BgpPeerConfig> parseBgpNeighbors(Router router, JsonObject peerMap) {
    HashSet<BgpPeerConfig> neighbors = new HashSet<>();
    for (Map.Entry<String, JsonElement> entry : peerMap.entrySet()) {
      JsonObject peerSettings = entry.getValue().getAsJsonObject();

      BgpPeerConfig.Builder peerBuilder = BgpPeerConfig.builder();

      JsonObject jsonIpv4UnicastAddressFamily =
          peerSettings.getAsJsonObject("ipv4UnicastAddressFamily");

      JsonObject jsonAddressFamilyCapabilities =
          jsonIpv4UnicastAddressFamily.getAsJsonObject("addressFamilyCapabilities");
      AddressFamilyCapabilities.Builder capBuilder = AddressFamilyCapabilities.builder();
      AddressFamilyCapabilities capabilities =
          capBuilder
              .setSendCommunity(
                  getAsBooleanDefaultFalse(jsonAddressFamilyCapabilities, "sendCommunity"))
              .setSendExtendedCommunity(
                  getAsBooleanDefaultFalse(jsonAddressFamilyCapabilities, "sendExtendedCommunity"))
              .build();

      Ipv4UnicastAddressFamily.Builder ipv4Builder = Ipv4UnicastAddressFamily.builder();
      Ipv4UnicastAddressFamily ipv4 =
          ipv4Builder
              .setAddressFamilyCapabilities(capabilities)
              .setEnable(true)
              .setRouteReflectorClient(
                  (getAsBooleanDefaultFalse(jsonIpv4UnicastAddressFamily, "reflectClient")))
              .setNextHopLocal(false)
              .setAllowAsLoop(0)
              .setPublicAsOnly(true)
              .setDefaultRouteAdvertise(false)
              .setWeight(getAsIntOrDefault(peerSettings, "defaultMetric", 0))
              .setImportPrefixFilter(null)
              .setImportCommunityFilter(null)
              .setImportAsPathFilter(null)
              .setImportRoutePolicy(
                  router.getRoutePolicy(
                      getAsStringDefaultNull(jsonIpv4UnicastAddressFamily, "importPolicy")))
              .setExportPrefixFilter(null)
              .setExportCommunityFilter(null)
              .setExportAsPathFilter(null)
              .setExportRoutePolicy(
                  router.getRoutePolicy(
                      getAsStringDefaultNull(jsonIpv4UnicastAddressFamily, "exportPolicy")))
              .build();

      Ip localIp =
          peerSettings.get("localIp").isJsonNull()
              ? null
              : Ip.of(peerSettings.get("localIp").getAsString());
      Interface connectInterface =
          router.getInterface(getAsStringDefaultNull(peerSettings, "connectInterface"));
      if (localIp == null && connectInterface != null) {
        localIp = connectInterface.getIp();
      }
      // From huawei doc:
      // If neither internal nor external is configured, an IBGP peer group is created by default.
      boolean external = getAsBooleanDefaultFalse(peerSettings, "external");
      BgpPeerConfig peerConfig =
          peerBuilder
              .setLocalIp(localIp)
              .setRemoteIp(Ip.of(peerSettings.get("peerAddress").getAsString()))
              .setLocalAS(parseAsNumber(peerSettings.get("localAs").getAsString()))
              .setRemoteAS(parseAsNumber(peerSettings.get("remoteAsns").getAsString()))
              .setConnectInterface(connectInterface)
              .setDescription(
                  getAsStringDefaultEmpty(peerSettings, "description")
                      .replace("\"", "")
                      .replace("|", "~")
                      .replace("/", "~"))
              .setGroup(getAsStringDefaultNull(peerSettings, "group"))
              .setIgnore(false)
              .setInternal(!external)
              .setExternal(external)
              .setEbgpMaxHop(getAsBooleanDefaultFalse(peerSettings, "ebgpMultihop") ? 10 : 0)
              .setIpv4UnicastAddressFamily(ipv4)
              .build();

      neighbors.add(peerConfig);
    }
    return neighbors;
  }

  public static long parseAsNumber(String asn) {
    if (asn.contains(".")) {
      String[] parts = asn.split("\\.");
      return Long.parseLong(parts[0]) * 65536 + Long.parseLong(parts[1]);
    } else {
      return Long.parseLong(asn);
    }
  }
}
