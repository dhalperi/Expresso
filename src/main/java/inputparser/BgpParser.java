package inputparser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.bgp.addressfamily.AddressFamilyCapabilities;
import controlplane.process.bgp.addressfamily.Ipv4UnicastAddressFamily;
import controlplane.process.bgp.routeaggregation.RouteAggregation;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Mask;
import datamodel.ipv4.Prefix;
import datamodel.routepolicy.RoutePolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import static datamodel.filterlist.FilterType.*;
import static util.JsonUtil.*;

public class BgpParser {

  public static BgpRoutingProcess bgpProcess;

  public static BgpRoutingProcess parseBgpProcess(VirtualRouter vrf, JsonObject jsonBgp) {
    BgpRoutingProcess.Builder builder = BgpRoutingProcess.builder();

    builder
        .setVirtualRouter(vrf)
        .setRouterId(Ip.of(getAsStringDefaultNull(jsonBgp, "routerId")))
        .setNeighbors(parseBgpNeighbors(vrf.getRouter(), jsonBgp.getAsJsonObject("neighbors")))
        .setNetworks(parseBgpNetworks(vrf, jsonBgp.getAsJsonArray("network")))
        .setAggregations(parseAggregations(jsonBgp.getAsJsonArray("aggregations")))
        .setPreference(parsePreference(jsonBgp.getAsJsonArray("preference")))
        .setDefaultLocalPreference(getAsIntOrDefault(jsonBgp, "defaultLocalPreference", 100))
        .setMaxLoadBalancingIbgp(getAsIntOrDefault(jsonBgp, "maxLoadBalancingIbgp", 1))
        .setMaxLoadBalancingEbgp(getAsIntOrDefault(jsonBgp, "maxLoadBalancingEbgp", 1))
        .setImports(RedistributionParser.parseImports(vrf, jsonBgp.getAsJsonArray("imports")));

    bgpProcess = builder.build();

    return bgpProcess;
  }

  public static HashSet<BgpPeerConfig> parseBgpNeighbors(Router router, JsonObject peerMap) {
    HashSet<BgpPeerConfig> neighbors = new HashSet<>();
    for (Map.Entry<String, JsonElement> entry : peerMap.entrySet()) {
      JsonObject peerSettings = entry.getValue().getAsJsonObject();

      BgpPeerConfig.Builder peerBuilder = BgpPeerConfig.builder();
      Ipv4UnicastAddressFamily.Builder ipv4Builder = Ipv4UnicastAddressFamily.builder();
      AddressFamilyCapabilities.Builder capBuilder = AddressFamilyCapabilities.builder();

      AddressFamilyCapabilities capabilities =
          capBuilder
              .setSendCommunity(getAsBooleanDefaultFalse(peerSettings, "advertiseCommunity"))
              .setSendExtendedCommunity(
                  getAsBooleanDefaultFalse(peerSettings, "advertiseExtCommunity"))
              .build();

      Ipv4UnicastAddressFamily ipv4 =
          ipv4Builder
              .setAddressFamilyCapabilities(capabilities)
              .setEnable(getAsBooleanDefaultFalse(peerSettings, "enable"))
              .setRouteReflectorClient(peerSettings.get("reflectClient").getAsBoolean())
              .setNextHopLocal(getAsBooleanDefaultFalse(peerSettings, "nextHopLocal"))
              .setAllowAsLoop(getAsIntOrDefault(peerSettings, "allowAsLoop", 0))
              .setPublicAsOnly(getAsBooleanDefaultTrue(peerSettings, "publicAsOnly"))
              .setDefaultRouteAdvertise(
                  getAsBooleanDefaultFalse(peerSettings, "defaultRouteAdvertise"))
              .setWeight(getAsIntOrDefault(peerSettings, "preferredValue", 0))
              .setImportPrefixFilter(
                  router.getFilterList(
                      PREFIX, getAsStringDefaultNull(peerSettings, "importPrefixFilter")))
              .setImportCommunityFilter(
                  router.getFilterList(
                      COMMUNITY, getAsStringDefaultNull(peerSettings, "importCommunityFilter")))
              .setImportAsPathFilter(
                  router.getFilterList(
                      AS_PATH, getAsStringDefaultNull(peerSettings, "importAsPathFilter")))
              .setImportRoutePolicy(
                  router.getRoutePolicy(getAsStringDefaultNull(peerSettings, "importRoutePolicy")))
              .setExportPrefixFilter(
                  router.getFilterList(
                      PREFIX, getAsStringDefaultNull(peerSettings, "exportPrefixFilter")))
              .setExportCommunityFilter(
                  router.getFilterList(
                      COMMUNITY, getAsStringDefaultNull(peerSettings, "exportCommunityFilter")))
              .setExportAsPathFilter(
                  router.getFilterList(
                      AS_PATH, getAsStringDefaultNull(peerSettings, "exportAsPathFilter")))
              .setExportRoutePolicy(
                  router.getRoutePolicy(getAsStringDefaultNull(peerSettings, "exportRoutePolicy")))
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
              .setRemoteIp(Ip.of(peerSettings.get("remoteIp").getAsString()))
              .setLocalAS(parseAsNumber(peerSettings.get("localAs").getAsString()))
              .setRemoteAS(parseAsNumber(peerSettings.get("remoteAs").getAsString()))
              .setConnectInterface(connectInterface)
              .setDescription(getAsStringDefaultEmpty(peerSettings, "description"))
              .setGroup(getAsStringDefaultNull(peerSettings, "group"))
              .setIgnore(getAsBooleanDefaultFalse(peerSettings, "ignore"))
              .setInternal(!external)
              .setExternal(external)
              .setEbgpMaxHop(getAsIntOrDefault(peerSettings, "ebgpMaxHop", 1))
              .setIpv4UnicastAddressFamily(ipv4)
              .build();

      neighbors.add(peerConfig);
    }
    return neighbors;
  }

  public static HashMap<RoutePolicy, HashSet<Prefix>> parseBgpNetworks(
      VirtualRouter vrf, JsonArray jsonNetworks) {
    HashMap<RoutePolicy, HashSet<Prefix>> networks = new HashMap<>();
    Multimap<RoutePolicy, Prefix> multimap = HashMultimap.create();
    if (jsonNetworks != null) {
      for (int i = 0; i < jsonNetworks.size(); i++) {
        JsonObject jsonNetwork = jsonNetworks.get(i).getAsJsonObject();
        Prefix network =
            Prefix.of(
                Ip.of(jsonNetwork.get("ip").getAsString()),
                Mask.of(jsonNetwork.get("mask").getAsString()));
        RoutePolicy routePolicy =
            vrf.getRouter().getRoutePolicy(getAsStringDefaultNull(jsonNetwork, "routePolicy"));
        multimap.put(routePolicy, network);
      }
    }
    multimap
        .asMap()
        .forEach(((routePolicy, prefixes) -> networks.put(routePolicy, new HashSet<>(prefixes))));
    return networks;
  }

  public static Vector<RouteAggregation> parseAggregations(JsonArray jsonAggrs) {
    Vector<RouteAggregation> aggrs = new Vector<>();
    if (jsonAggrs != null) {
      for (int i = 0; i < jsonAggrs.size(); i++) {
        JsonObject jsonAggr = jsonAggrs.get(i).getAsJsonObject();
        Prefix prefix =
            Prefix.of(
                Ip.of(jsonAggr.get("ip").getAsString()),
                Mask.of(jsonAggr.get("mask").getAsString()));
        aggrs.add(new RouteAggregation(prefix, false, false, null, null, null));
      }
    }
    return aggrs;
  }

  public static int[] parsePreference(JsonArray jsonPreference) {
    int[] preference = new int[3];
    for (int i = 0; i < 3; i++) {
      preference[i] = jsonPreference.get(i).getAsInt();
    }
    return preference;
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
