package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import inputparser.bfvi.PeerIpParser;
import main.Controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Statistics {
  public static int numOspfRoutes() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .flatMap(vr -> vr.getOspfProcesses().values().stream())
        .map(ospf -> ospf.getRib().getAllRoutes().size())
        .reduce(0, Integer::sum);
  }

  public static int numIsisRoutes() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .map(VirtualRouter::getIsisProcess)
        .filter(Objects::nonNull)
        .map(isis -> isis.getRib().getAllRoutes().size())
        .reduce(0, Integer::sum);
  }

  public static int numBgpRoutes() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .map(VirtualRouter::getBgpProcess)
        .filter(Objects::nonNull)
        .map(bgp -> bgp.getRib().getAllRoutes().size())
        .reduce(0, Integer::sum);
  }

  public static int numForwardingRules() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .flatMap(n -> n.getVirtualRouters().values().stream())
        .map(vr -> vr.getFib().getEntries().size())
        .reduce(0, Integer::sum);
  }

  public static Map<String, Map<String, Integer>> numOspfRoutesInDetail() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .collect(
            Collectors.toMap(
                Router::getRouterName,
                n ->
                    n.getVirtualRouters().values().stream()
                        .collect(
                            Collectors.toMap(
                                VirtualRouter::getVrfName,
                                vr ->
                                    vr.getOspfProcesses().values().stream()
                                        .map(ospf -> ospf.getRib().getAllRoutes().size())
                                        .reduce(0, Integer::sum)))));
  }

  public static Map<String, Map<String, Integer>> numIsisRoutesInDetail() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .collect(
            Collectors.toMap(
                Router::getRouterName,
                n ->
                    n.getVirtualRouters().values().stream()
                        .collect(
                            Collectors.toMap(
                                VirtualRouter::getVrfName,
                                vr ->
                                    vr.getIsisProcess() == null
                                        ? 0
                                        : vr.getIsisProcess().getRib().getAllRoutes().size()))));
  }

  public static Map<String, Map<String, Integer>> numBgpRoutesInDetail() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .collect(
            Collectors.toMap(
                Router::getRouterName,
                n ->
                    n.getVirtualRouters().values().stream()
                        .collect(
                            Collectors.toMap(
                                VirtualRouter::getVrfName,
                                vr ->
                                    vr.getBgpProcess() == null
                                        ? 0
                                        : vr.getBgpProcess().getRib().getAllRoutes().size()))));
  }

  public static Map<String, Map<String, Integer>> numForwardingRulesInDetail() {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .collect(
            Collectors.toMap(
                Router::getRouterName,
                n ->
                    n.getVirtualRouters().values().stream()
                        .collect(
                            Collectors.toMap(
                                VirtualRouter::getVrfName,
                                vr -> vr.getFib().getEntries().size()))));
  }

  public static long numConfigurationLines(Path path) {
    if (path.toFile().isDirectory()) {
      String[] files = path.toFile().list();
      long num =
          Arrays.stream(Objects.requireNonNull(files))
              .filter(file -> file.endsWith(".cfg") || file.endsWith("conf"))
              .mapToLong(file -> numConfigurationLines(path.resolve(file)))
              .sum();
      System.out.printf("%s: %d\n", path.getParent().getFileName(), num);
      return num;
    } else {
      try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
        long num = br.lines().count();
        System.out.printf("%s: %d\n", path.getFileName(), num);
        return num;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return 0;
    }
  }

  public static void numInternet2Peers(Path path) {
    TreeSet<String> bagpipePeers =
        PeerIpParser.parsePeerIps(path.getParent().resolve("bagpipe-peers.txt")).stream()
            .map(ip -> ip.toString() + "/32")
            .collect(Collectors.toCollection(TreeSet::new));

    File cfgDir = path.toFile();
    if (cfgDir.isDirectory()) {
      String[] cfgFiles = cfgDir.list();
      assert cfgFiles != null;

      TreeMap<String, TreeSet<String>> customers = new TreeMap<>();
      TreeMap<String, TreeSet<String>> peers = new TreeMap<>();
      for (String cfgFile : cfgFiles) {
        JsonParser parser = new JsonParser();
        try {
          JsonObject root =
              (JsonObject) parser.parse(new FileReader(path.resolve(cfgFile).toFile()));
          JsonObject jsonVrfs = root.getAsJsonObject("vrfs");
          for (Map.Entry<String, JsonElement> e0 : jsonVrfs.entrySet()) {
            JsonObject jsonVrf = e0.getValue().getAsJsonObject();
            if (jsonVrf.get("bgpProcess").isJsonNull()) continue;
            JsonObject jsonBgp = jsonVrf.getAsJsonObject("bgpProcess");
            if (jsonBgp != null) {
              JsonObject jsonNeighbors = jsonBgp.getAsJsonObject("neighbors");
              for (Map.Entry<String, JsonElement> e1 : jsonNeighbors.entrySet()) {
                JsonArray jsonImport =
                    e1.getValue()
                        .getAsJsonObject()
                        .getAsJsonObject("ipv4UnicastAddressFamily")
                        .getAsJsonArray("importPolicySources");
                for (int j = 0; j < jsonImport.size(); j++) {
                  String str = jsonImport.get(j).getAsString();
                  if (str.equals("SET-PREF"))
                    customers
                        .computeIfAbsent(e1.getKey(), ip -> new TreeSet<>())
                        .add(root.get("name").getAsString() + "-" + e0.getKey());
                  if (str.equals("SET-PREF-PEER"))
                    peers
                        .computeIfAbsent(e1.getKey(), ip -> new TreeSet<>())
                        .add(root.get("name").getAsString() + "-" + e0.getKey());
                }
              }
            }
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
      System.out.printf(
          "nCustomer: %d, nPeer: %d\n", customers.keySet().size(), peers.keySet().size());
      System.out.println(peers.keySet().equals(bagpipePeers) ? "same peers" : "different peers");
      System.out.println(
          customers.entrySet().stream()
              .map(e -> String.format("%s: %s", e.getKey(), String.join(", ", e.getValue())))
              .collect(Collectors.joining("\n")));
      System.out.println("\n\n");
      System.out.println(
          peers.entrySet().stream()
              .map(e -> String.format("%s: %s", e.getKey(), String.join(", ", e.getValue())))
              .collect(Collectors.joining("\n")));
    }
  }

  public static void main(String... args) {
    numInternet2Peers(Paths.get("networks/internet2/configs"));
  }
}
