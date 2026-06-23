package propertycheckers.forwarding;

import propertycheckers.Checker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.stream.JsonWriter;
import controlplane.network.VirtualRouter;
import datamodel.ipv4.Prefix;
import datamodel.path.PathHop;
import dataplane.analysis.DPAnalyzer;
import dataplane.analysis.ReachabilityNode;
import main.Configuration;
import main.Controller;
import util.BddUtil;
import util.JsonUtil;
import util.MemorizeUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ForwardingChecker extends Checker {
  public static boolean INBOUND_REACH = false;
  public static boolean OUTBOUND_REACH = false;
  public static boolean TRAFFIC_HIJACK = true;
  public static boolean LOOP = false;
  public static boolean BLACKHOLE = false;

  private static Collection<ReachabilityNode> inboundReaches;
  private static Collection<ReachabilityNode> outboundReaches;
  private static Collection<ReachabilityNode> hijacks;
  private static Collection<ReachabilityNode> loops;
  private static Collection<ReachabilityNode> blackholes;

  public static boolean checkOrigin(PathHop origin) {
    if (origin instanceof VirtualRouter) {
      return ((VirtualRouter) origin).getVrfName().equals(Configuration.DEFAULT_VRF_NAME);
    }
    return false;
  }

  protected static int internal = -1;

  protected static int getInternalTraffic() {
    if (internal == -1) {
      if (INTERNAL_PREFIXES == null || INTERNAL_PREFIXES.isEmpty()) loadInternalPrefixes();
      Collection<Integer> col =
          INTERNAL_PREFIXES.stream()
              .map(
                  pfx ->
                      Controller.bddManager
                          .getBddPrefixWrapper()
                          .encodeIpWildcard(pfx.getIp(), pfx.getPreLength()))
              .collect(Collectors.toSet());
      internal = BddUtil.orInBatch(Controller.bddManager.getBDD(), col);
    }
    return internal;
  }

  public static void check(DPAnalyzer dpAnalyzer) {
    check(dpAnalyzer, INBOUND_REACH, OUTBOUND_REACH, TRAFFIC_HIJACK, LOOP, BLACKHOLE);
  }

  public static void check(
      DPAnalyzer dpAnalyzer,
      boolean inbound,
      boolean outbound,
      boolean hijack,
      boolean loop,
      boolean blackhole) {
    loadInternalPrefixes();
    inboundReaches =
        inbound ? InboundReachability.checkInboundReachability(dpAnalyzer) : Collections.emptySet();
    outboundReaches =
        outbound
            ? OutboundReachability.checkOutboundReachability(dpAnalyzer)
            : Collections.emptySet();
    hijacks = hijack ? TrafficHijackChecker.checkTrafficLeak(dpAnalyzer) : Collections.emptySet();
    loops = loop ? LoopChecker.checkLoop(dpAnalyzer) : Collections.emptySet();
    blackholes = blackhole ? BlackholeChecker.checkBlackhole(dpAnalyzer) : Collections.emptySet();
  }

  public static void print() {
    print(DETAILS, SUMMARY);
  }

  public static void print(boolean details, boolean summary) {
    OUTPUT =
        Controller.storage.output._outputBase.resolve(
            "dp_anomalies_" + Controller.cpExecutor.bgpTopology.getISPs().size());
    MemorizeUtil.createDirIfAbsentElseClean(OUTPUT.toFile());
    if (details) printDetails();
    if (summary) printSummary();
  }

  private static void printDetails() {
    Controller.cpExecutor.l3Topology.getNodes().keySet().forEach(ForwardingChecker::printDetails);
  }

  private static Collection<ReachabilityNode> getNodes(
      Collection<ReachabilityNode> nodes, String router) {
    return nodes.stream()
        .filter(
            node -> {
              Optional<PathHop> origin = node.getPath().getOrigin();
              return origin.isPresent() && origin.get().hopString().contains(router);
            })
        .collect(Collectors.toList());
  }

  private static void printDetails(String router) {
    Collection<ReachabilityNode> curInbounds = getNodes(inboundReaches, router);
    Collection<ReachabilityNode> curOutbounds = getNodes(outboundReaches, router);
    Collection<ReachabilityNode> curLeaks = getNodes(hijacks, router);
    Collection<ReachabilityNode> curLoops = getNodes(loops, router);
    Collection<ReachabilityNode> curBlackholes = getNodes(blackholes, router);
    if (Stream.of(curInbounds, curOutbounds, curLeaks, curLoops, curBlackholes)
        .anyMatch(col -> !col.isEmpty())) {
      print(
          router + ".json",
          ImmutableList.of(
              "inboundReachability", "outboundReachability", "trafficHijack", "loop", "blackhole"),
          ImmutableList.of(curInbounds, curOutbounds, curLeaks, curLoops, curBlackholes));
    }
  }

  private static void printSummary() {
    print(
        "summary.json",
        ImmutableList.of(
            "inboundReachability", "outboundReachability", "trafficLeak", "loop", "blackhole"),
        ImmutableList.of(inboundReaches, outboundReaches, hijacks, loops, blackholes));
  }

  private static void print(
      String fileName, List<String> anomalyNames, List<Collection<ReachabilityNode>> anomalyNodes) {
    if (anomalyNames.size() != anomalyNodes.size()) {
      throw new IllegalArgumentException();
    }

    if (USE_JACKSON) {
      // use jackson
      printJackson(fileName, anomalyNames, anomalyNodes);
    } else {
      // use gson
      try {
        RESULT_WRITER = new JsonWriter(new FileWriter(OUTPUT.resolve(fileName).toFile()));
        RESULT_WRITER.setIndent(" ");
        RESULT_WRITER.beginObject();

        IntStream.range(0, anomalyNames.size())
            .forEach(i -> printGson(RESULT_WRITER, anomalyNames.get(i), anomalyNodes.get(i)));

        RESULT_WRITER.endObject();
        RESULT_WRITER.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void printGson(
      JsonWriter jw, String type, Collection<ReachabilityNode> anomalies) {
    if (anomalies == null || anomalies.isEmpty()) {
      JsonUtil.writeEmptyArray(jw, type);
    } else {
      List<ReachabilityNode> list =
          anomalies.stream()
              .sorted(Comparator.comparing(node -> node.getPath().toString()))
              .collect(Collectors.toList());
      JsonUtil.writeArray(jw, type, list);
    }
  }

  private static void printJackson(
      String fileName, List<String> anomalyNames, List<Collection<ReachabilityNode>> anomalyNodes) {
    SortedMap<String, Object> anomalies = new TreeMap<>();
    IntStream.range(0, anomalyNames.size())
        .forEach(
            i ->
                anomalies.put(
                    anomalyNames.get(i) + "Num", sortAnomalies(anomalyNodes.get(i)).size()));
    IntStream.range(0, anomalyNames.size())
        .forEach(i -> anomalies.put(anomalyNames.get(i), sortAnomalies(anomalyNodes.get(i))));

    try (BufferedWriter bw =
        new BufferedWriter(new FileWriter(OUTPUT.resolve(fileName).toFile()))) {
      bw.write(JsonUtil.mapper.writeValueAsString(anomalies));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static boolean sample = false;

  private static SortedMap<String, ?> sortAnomalies(Collection<ReachabilityNode> nodes) {
    if (sample) {
      SortedMap<String, SortedMap<String, List<SortedMap<String, Object>>>> anomalies =
          new TreeMap<>();
      nodes.forEach(
          node -> {
            datamodel.path.Path path = node.getPath();
            if (path.size() > 0) {
              Prefix[] prefix = new Prefix[1];
              Object[] owners = new Object[1];
              node.getPrefixes().stream()
                  .filter(
                      pr ->
                          Controller.cpExecutor
                              .l3Topology
                              .getIpv4Owners()
                              .findOwners(pr)
                              .keySet()
                              .stream()
                              .anyMatch(ip -> !ip.equals("0.0.0.0/0")))
                  .findFirst()
                  .ifPresent(
                      pr -> {
                        prefix[0] = pr;
                        owners[0] = Controller.cpExecutor.l3Topology.getIpv4Owners().findOwners(pr);
                      });

              SortedMap<String, Object> attrs =
                  ImmutableSortedMap.of("path", path, "prefix", prefix[0], "owners", owners[0]);
              anomalies
                  .computeIfAbsent(path.get(0).hopString(), src -> new TreeMap<>())
                  .computeIfAbsent(path.get(path.size() - 1).hopString(), dst -> new LinkedList<>())
                  .add(attrs);
            }
          });
      return anomalies;
    } else {
      SortedMap<String, SortedMap<String, List<ReachabilityNode>>> anomalies = new TreeMap<>();
      nodes.forEach(
          node -> {
            datamodel.path.Path path = node.getPath();
            String src = path.get(0).hopString();
            String dst = path.get(path.size() - 1).hopString();
            anomalies
                .computeIfAbsent(src, s -> new TreeMap<>())
                .computeIfAbsent(dst, d -> new LinkedList<>())
                .add(node);
          });
      return anomalies;
    }
  }

  public static class ForwardingCheckResult {
    public final Collection<ReachabilityNode> inboundReaches;
    public final Collection<ReachabilityNode> outboundReaches;
    public final Collection<ReachabilityNode> hijacks;
    public final Collection<ReachabilityNode> loops;
    public final Collection<ReachabilityNode> blackholes;

    public ForwardingCheckResult(
        Collection<ReachabilityNode> inboundReaches,
        Collection<ReachabilityNode> outboundReaches,
        Collection<ReachabilityNode> hijacks,
        Collection<ReachabilityNode> loops,
        Collection<ReachabilityNode> blackholes) {
      this.inboundReaches = inboundReaches;
      this.outboundReaches = outboundReaches;
      this.hijacks = hijacks;
      this.loops = loops;
      this.blackholes = blackholes;
    }
  }

  public static ForwardingCheckResult getResult() {
    return new ForwardingCheckResult(inboundReaches, outboundReaches, hijacks, loops, blackholes);
  }
}
