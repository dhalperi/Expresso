package inputparser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import controlplane.network.L3Topology;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.aspath.AsPathFactory;
import datamodel.aspath.AsPathIntf;
import datamodel.community.CommunityListFactory;
import datamodel.route.RouteSet;
import javafx.util.Pair;
import main.Controller;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExternalRouteParser {
  private static class ExternalRoute {
    final Prefix destination;
    final int preference;
    final int localPreference;
    final AsPath asPath;
    final Ip learnedFrom;

    public ExternalRoute(
        Prefix destination, int preference, int localPreference, AsPath asPath, Ip learnedFrom) {
      this.destination = destination;
      this.preference = preference;
      this.localPreference = localPreference;
      this.asPath = asPath;
      this.learnedFrom = learnedFrom;
    }

    public BgpRoute toBgpRoute() {
      datamodel.ipv4.Prefix pfx = datamodel.ipv4.Prefix.of(destination.toString());
      AsPathIntf asp =
          AsPathFactory.ofSingletonAsSets(
              asPath.getAsSets().stream()
                  .flatMap(asSet -> asSet.getAsns().stream())
                  .collect(Collectors.toList()));
      datamodel.ipv4.Ip nh = datamodel.ipv4.Ip.parse(learnedFrom.toString());

      return new BgpRouteBuilder()
          .setPrefixesBdd(Controller.bddManager.getBddPrefixWrapper().encodePrefix(pfx))
          .setNextHopIp(nh)
          .setPath(new datamodel.path.Path())
          .setAsPath(asp)
          .setCommunityList(CommunityListFactory.empty())
          .setLocalPreference(localPreference)
          .setType(BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR)
          .build();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExternalRoute that = (ExternalRoute) o;
      return preference == that.preference
          && localPreference == that.localPreference
          && Objects.equals(destination, that.destination)
          && Objects.equals(asPath, that.asPath)
          && Objects.equals(learnedFrom, that.learnedFrom);
    }

    @Override
    public int hashCode() {
      return Objects.hash(destination, preference, localPreference, asPath, learnedFrom);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("destination", destination)
          .add("preference", preference)
          .add("localPreference", localPreference)
          .add("asPath", asPath)
          .add("learnedFrom", learnedFrom)
          .toString();
    }
  }

  public static void parseAndSetKnownExternalRoutes(Path path, L3Topology l3Topology) {
    List<ExternalRoute> list = parseExternalRoutes(path);
    tryFindExternalPeerConfigs(list, l3Topology);
  }

  private static List<ExternalRoute> parseExternalRoutes(Path path) {
    List<ExternalRoute> externalRoutes = new LinkedList<>();
    try {
      SAXReader reader = new SAXReader();
      Document document = reader.read(path.toFile());
      Element root = document.getRootElement();

      List<Element> entries =
          root.element("router").element("route-information").element("route-table").elements("rt");
      for (Element entry : entries) {
        ExternalRoute externalRoute = parseExternalRoute(entry);
        if (externalRoute != null) externalRoutes.add(externalRoute);
      }
    } catch (DocumentException e) {
      e.printStackTrace();
    }
    return externalRoutes;
  }

  private static ExternalRoute parseExternalRoute(Element element) {
    Element child = element.element("rt-entry");

    String protocol = child.elementText("protocol-name");
    if (!protocol.equals("BGP")) return null;

    String learnedFrom = child.elementText("learned-from");
    if (learnedFrom == null) return null;

    String destination = element.element("rt-destination").getText();
    String preference = child.elementText("preference");
    String localPreference = child.elementText("local-preference");

    String asPath = child.elementText("as-path");
    int idx = 0;
    for (int c : asPath.chars().boxed().collect(Collectors.toList())) {
      if (c == ' ' || (c >= '0' && c <= '9')) idx++;
      else break;
    }
    asPath = asPath.substring(0, idx);

    return new ExternalRoute(
        Prefix.parse(destination),
        Integer.parseInt(preference),
        Integer.parseInt(localPreference),
        AsPath.ofSingletonAsSets(
            Arrays.stream(asPath.split(" "))
                .mapToLong(Long::parseLong)
                .boxed()
                .collect(Collectors.toList())),
        Ip.parse(learnedFrom));
  }

  private static List<Pair<ExternalRoute, BgpPeerConfig>> tryFindExternalPeerConfigs(
      List<ExternalRoute> externalRoutes, L3Topology l3Topology) {
    ImmutableList.Builder<Pair<ExternalRoute, BgpPeerConfig>> builder = ImmutableList.builder();
    for (ExternalRoute externalRoute : externalRoutes) {
      List<BgpPeerConfig> peerConfigs = tryFindExternalPeerConfig(externalRoute, l3Topology);
      peerConfigs.forEach(peerConfig -> builder.add(new Pair<>(externalRoute, peerConfig)));
    }
    ImmutableList<Pair<ExternalRoute, BgpPeerConfig>> list = builder.build();

    HashMap<BgpRoutingProcess, HashMap<BgpPeerConfig, RouteSet<BgpRoute>>> map = new HashMap<>();
    list.forEach(
        pair ->
            map.computeIfAbsent(pair.getValue().getProcess(), p -> new HashMap<>())
                .computeIfAbsent(pair.getValue(), c -> new RouteSet<>())
                .add(pair.getKey().toBgpRoute()));
    map.forEach(BgpRoutingProcess::setKnownExternalRoutes);

    return list;
  }

  private static List<BgpPeerConfig> tryFindExternalPeerConfig(
      ExternalRoute externalRoute, L3Topology l3Topology) {
    Router newy = l3Topology.getNodes().get("newy-re1");
    if (externalRoute.learnedFrom == null) {
      // find in newy
      return Collections.singletonList(
          tryFindExternalPeerConfig(externalRoute, newy.getVirtualRouters().get("default")));
    } else {
      Collection<VirtualRouter> vrs =
          l3Topology
              .getIpOwners()
              .get(datamodel.ipv4.Ip.parse(externalRoute.learnedFrom.toString()));
      return vrs.stream()
          .map(vr -> tryFindExternalPeerConfig(externalRoute, vr))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

  private static BgpPeerConfig tryFindExternalPeerConfig(
      ExternalRoute externalRoute, VirtualRouter vr) {
    return vr.getBgpProcess().getNeighbors().stream()
        .filter(peer -> externalRoute.asPath.getAsSets().get(0).containsAs(peer.getRemoteAS()))
        .findFirst()
        .orElse(null);
  }

  public static List<Map<String, Object>> buildExternalAdvertisementForBatfish(
      List<Pair<ExternalRoute, BgpPeerConfig>> list) {
    ImmutableList.Builder<Map<String, Object>> builder = ImmutableList.builder();

    for (Pair<ExternalRoute, BgpPeerConfig> pair : list) {
      ExternalRoute externalRoute = pair.getKey();
      BgpPeerConfig peerConfig = pair.getValue();

      Map<String, Object> map = new TreeMap<>();
      map.put("type", "ebgp_sent");
      map.put("network", externalRoute.destination);
      map.put("nextHopIp", peerConfig.getRemoteIp().toString());
      map.put("srcNode", String.format("AS%d", peerConfig.getRemoteAS()));
      map.put("srcIp", peerConfig.getRemoteIp().toString());
      map.put("dstNode", peerConfig.getProcess().getVrf().getRouter().getRouterName());
      map.put("dstIp", getStringOrNull(peerConfig, BgpPeerConfig::getLocalIp));
      map.put("srcProtocol", "AGGREGATE");
      map.put("originType", "incomplete");
      map.put("localPreference", externalRoute.localPreference);
      map.put("med", 0);
      map.put("originatorIp", "0.0.0.0");
      map.put("asPath", externalRoute.asPath);
      map.put("communities", Collections.emptyList());
      map.put("srcVrf", "default");
      map.put("dstVrf", peerConfig.getProcess().getVrf().getVrfName());
      map.put("clusterList", Collections.emptyList());

      builder.add(map);
    }

    return builder.build();
  }

  public static String getStringOrNull(BgpPeerConfig obj, Function<BgpPeerConfig, Object> func) {
    if (func.apply(obj) == null) return null;
    return Objects.toString(func.apply(obj));
  }
}
