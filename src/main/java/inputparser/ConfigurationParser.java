package inputparser;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.route.RoutingProtocol;
import datamodel.acl.Acl;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.ipv4.Prefix;
import datamodel.routepolicy.RoutePolicy;
import datamodel.trafficpolicy.TrafficPolicy;
import datamodel.trafficpolicy.behavior.TrafficBehavior;
import datamodel.trafficpolicy.classifier.TrafficClassifier;
import datamodel.vendor.Cisco;
import datamodel.vendor.Huawei;
import datamodel.vendor.Juniper;
import datamodel.vendor.Vendor;
import main.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import static util.JsonUtil.getAsObjectDefaultNull;

public class ConfigurationParser {
  public static HashMap<String, RoutingProtocol> protocolMap;

  static {
    protocolMap = new HashMap<>();
    protocolMap.put("connected", RoutingProtocol.CONNECTED);
    protocolMap.put("static", RoutingProtocol.STATIC);
    protocolMap.put("ospf", RoutingProtocol.OSPF);
    protocolMap.put("bgp", RoutingProtocol.BGP);
  }

  TreeMap<String, Router> routers;
  TreeMap<InterfaceName, Interface> interfaces;
  TreeSet<Long> internalASNs;
  TreeSet<Prefix> internalPrefixes;

  public ConfigurationParser() {
    routers = new TreeMap<>();
    interfaces = new TreeMap<>();
    internalASNs = new TreeSet<>();
    internalPrefixes = new TreeSet<>();
  }

  public void parse(Path cfgs) {
    File cfgDir = cfgs.toFile();
    if (cfgDir.isDirectory()) {
      String[] cfgFiles = cfgDir.list();
      assert cfgFiles != null;
      for (String cfgFile : cfgFiles) {
        Router router = parseFile(cfgs.resolve(cfgFile));
        routers.put(router.getRouterName(), router);
        interfaces.putAll(router.getInterfaces());
      }
      collectInternalASNs(cfgs.getParent().resolve("information.json"));
      collectInternalPrefixes(cfgs.getParent().resolve("information.json"));
    }
  }

  public Router parseFile(Path cfg) {
    Router router = null;
    JsonParser parser = new JsonParser();
    try {
      JsonObject root = (JsonObject) parser.parse(new FileReader(cfg.toFile()));

      boolean bfvi = root.has("communitySetMatchExprs");

      Vendor vendor = parseVendor(root.getAsJsonObject("vendorFamily"));
      Configuration.DEFAULT_VENDOR = vendor;

      // parse router
      String name = cfg.getFileName().toString().replace(".json", "");
      router = new Router(name);

      // parse ACLs
      SortedMap<String, Acl> acls = AclParser.parseAcls(root.getAsJsonObject("ipAccessLists"));
      router.setAcls(acls);

      // parse traffic classifiers
      SortedMap<String, TrafficClassifier> trafficClassifiers =
          PbrParser.parseTrafficClassifiers(router, root.getAsJsonObject("packetClassifiers"));
      router.setTrafficClassifiers(trafficClassifiers);

      // parse traffic behaviors
      SortedMap<String, TrafficBehavior> trafficBehaviors =
          PbrParser.parseTrafficBehaviors(root.getAsJsonObject("packetBehaviors"));
      router.setTrafficBehaviors(trafficBehaviors);

      // parse traffic policies
      SortedMap<String, TrafficPolicy> trafficPolicies =
          PbrParser.parseTrafficPolicies(router, root.getAsJsonObject("packetPolicies"));
      router.setTrafficPolicies(trafficPolicies);

      // parse interfaces
      Multimap<String, InterfaceName> vrfInterfaces =
          InterfaceParser.parseInterfaces(router, root.getAsJsonObject("interfaces"));

      // parse filter lists
      SortedMap<String, FilterList> prefixLists =
          FilterListParser.parsePrefixLists(root.getAsJsonObject("routeFilterLists"));
      SortedMap<String, FilterList> communityLists =
          FilterListParser.parseCommunityLists(root.getAsJsonObject("communityLists"));
      SortedMap<String, FilterList> asPathLists =
          FilterListParser.parseAsPathLists(root.getAsJsonObject("asPathAccessLists"));
      Table<FilterType, String, FilterList> filterLists = HashBasedTable.create();
      prefixLists.forEach((key, value) -> filterLists.put(FilterType.PREFIX, key, value));
      communityLists.forEach((key, value) -> filterLists.put(FilterType.COMMUNITY, key, value));
      asPathLists.forEach((key, value) -> filterLists.put(FilterType.AS_PATH, key, value));
      router.setFilterLists(filterLists);

      // parse route policies
      SortedMap<String, RoutePolicy> routePolicies =
          bfvi
              ? inputparser.bfvi.RoutePolicyParser.parseRoutePolicies(
                  router, root.getAsJsonObject("routingPolicies"), root)
              : RoutePolicyParser.parseRoutePolicies(
                  router, root.getAsJsonObject("routingPolicies"));
      router.setRoutePolicies(routePolicies);

      // parse vrfs
      SortedMap<String, VirtualRouter> vrfs =
          bfvi
              ? inputparser.bfvi.VrfParser.parseVrfs(
                  router, root.getAsJsonObject("vrfs"), vrfInterfaces)
              : VrfParser.parseVrfs(router, root.getAsJsonObject("vrfs"));
      router.setVirtualRouters(vrfs);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return router;
  }

  Vendor parseVendor(JsonObject vendor) {
    if (getAsObjectDefaultNull(vendor, "huawei", JsonElement::getAsJsonObject) != null) {
      Configuration.DEFAULT_VENDOR = Huawei.HUAWEI;
    } else if (getAsObjectDefaultNull(vendor, "juniper", JsonElement::getAsJsonObject) != null) {
      Configuration.DEFAULT_VENDOR = Juniper.JUNIPER;
    } else if (getAsObjectDefaultNull(vendor, "cisco", JsonElement::getAsJsonObject) != null
        || getAsObjectDefaultNull(vendor, "cisco_nxos", JsonElement::getAsJsonObject) != null) {
      Configuration.DEFAULT_VENDOR = Cisco.CISCO;
    }
    return Configuration.DEFAULT_VENDOR;
  }

  void collectInternalASNs(Path info) {
    if (Configuration.DEFAULT_VENDOR instanceof Huawei && !info.toFile().exists()) {
      for (Router router : routers.values()) {
        for (VirtualRouter vrf : router.getVirtualRouters().values()) {
          if (vrf.getBgpProcess() != null) {
            for (BgpPeerConfig peerConfig : vrf.getBgpProcess().getNeighbors()) {
              internalASNs.add(peerConfig.getLocalAS());
              // peers belong to a certain peer-group are usually internal peers
              if (peerConfig.getRemoteAS() != peerConfig.getLocalAS()
                  && peerConfig.getGroup() != null) {
                internalASNs.add(peerConfig.getRemoteAS());
              }
            }
          }
        }
      }
    }
  }

  void collectInternalPrefixes(Path info) {
    if (Configuration.DEFAULT_VENDOR instanceof Huawei && !info.toFile().exists()) {
      for (Router router : routers.values()) {
        for (VirtualRouter vrf : router.getVirtualRouters().values()) {
          if (vrf.getBgpProcess() != null) {
            BgpRoutingProcess bgpProcess = vrf.getBgpProcess();
            bgpProcess.getNetworks().values().forEach(internalPrefixes::addAll);
            for (BgpPeerConfig peerConfig : vrf.getBgpProcess().getNeighbors()) {
              // add whitelists
            }
          }
        }
      }
    }
  }

  public TreeMap<String, Router> getRouters() {
    return routers;
  }

  public TreeMap<InterfaceName, Interface> getInterfaces() {
    return interfaces;
  }

  public TreeSet<Long> getInternalASNs() {
    return internalASNs;
  }

  public TreeSet<Prefix> getInternalPrefixes() {
    return internalPrefixes;
  }
}
