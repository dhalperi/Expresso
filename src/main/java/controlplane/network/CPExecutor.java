package controlplane.network;

import atomic.ConfigAtomicPredicates;
import atomic.automaton.AutomatonAtomicPredicates;
import atomic.automaton.AutomatonRepresented;
import atomic.automaton.AutomatonRepresentedImpl;
import bdd.AtomManager;
import bdd.BddAtomManager;
import controlplane.process.bgp.BgpExecutor;
import controlplane.process.bgp.BgpTopology;
import controlplane.process.isis.IsisExecutor;
import controlplane.process.isis.IsisTopology;
import controlplane.process.ospf.OspfExecutor;
import controlplane.process.ospf.OspfTopology;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.path.BTE;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetAsPath;
import dk.brics.automaton.Automaton;
import inputparser.ConfigurationParser;
import inputparser.TopologyParser;
import inputparser.ExternalRouteParser;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import util.Statistics;
import util.TimeUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CPExecutor {
  public L3Topology l3Topology;
  public OspfTopology ospfTopology;
  public IsisTopology isisTopology;
  public BgpTopology bgpTopology;

  public AutomatonAtomicPredicates<CommunityRegex> communityAPs;
  public AtomManager<CommunityRegex, Automaton> communityManager;

  public AutomatonAtomicPredicates<AsPathRegex> asPathAPs;
  public AtomManager<AsPathRegex, Automaton> asPathManager;

  public CPExecutor() {}

  public void init() {
    ConfigurationParser parser = new ConfigurationParser();
    parser.parse(Controller.storage.input._config);
    l3Topology =
        new L3Topology(
            parser.getRouters(),
            parser.getInterfaces(),
            TopologyParser.parseL3Edges(Controller.storage.input._l3Edges));
    ospfTopology = new OspfTopology();
    isisTopology = new IsisTopology();
    bgpTopology = new BgpTopology(parser.getInternalASNs(), parser.getInternalPrefixes());

    initAtoms();

    Path known = Controller.storage.input._inputBase.resolve("knownExternalRoutes.xml");
    if (known.toFile().exists()) {
      ExternalRouteParser.parseAndSetKnownExternalRoutes(known, l3Topology);
    }
  }

  private void initAtoms() {
    logInitAtoms();
    initCommunityAtoms();
    initAsPathAtoms();
  }

  private void logInitAtoms() {
    String log =
        String.format(
            "%s, %s",
            Configuration.SYMBOLIC_COMMUNITY
                ? "symbolic community, "
                    + (Configuration.SYMBOLIC_COMMUNITY_AP ? "using AP" : "using automata")
                : "concrete community",
            Configuration.SYMBOLIC_AS_PATH
                ? "symbolic AS path, "
                    + (Configuration.SYMBOLIC_AS_PATH_AP ? "using AP" : "using automata")
                : "concrete AS path");
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, log);
  }

  private void initCommunityAtoms() {
    if (Configuration.SYMBOLIC_COMMUNITY && Configuration.SYMBOLIC_COMMUNITY_AP) {
      Set<CommunityRegex> communityRegexes = ConfigAtomicPredicates.findAllCommunities(true);
      communityRegexes.add(BTE.getCommunityRegex());
      communityAPs =
          new AutomatonAtomicPredicates<>(
              communityRegexes, CommunityRegex.ALL_STANDARD_COMMUNITIES);
      ExpressoLogger.log(
          ExpressoLogger.LEVEL.INFO,
          String.format(
              "community: n_p = %d, n_ap = %d",
              communityRegexes.size(), communityAPs.getNumAtoms()));
      communityManager = new BddAtomManager<>(communityAPs);
    }
  }

  public static boolean initAsPathFiltersWholly = false;

  private void initAsPathAtoms() {
    if (Configuration.SYMBOLIC_AS_PATH && Configuration.SYMBOLIC_AS_PATH_AP) {
      if (initAsPathFiltersWholly) {
        // View the entire AS path list as an AS path regex
        Set<FilterList> asPathLists =
            ConfigAtomicPredicates.findAllFilterListsOfType(FilterType.AS_PATH, true);
        Set<AutomatonRepresented> origins1 =
            asPathLists.stream()
                .map(ConfigAtomicPredicates::convertAsPathListToAutomaton)
                .collect(Collectors.toSet());

        Set<Action> asPathActions =
            ConfigAtomicPredicates.findAllRoutePolicyActionsOfClass(SetAsPath.class, true);
        Set<AutomatonRepresented> origins2 =
            asPathActions.stream()
                .flatMap(
                    a -> ConfigAtomicPredicates.convertSetAsPathToAutomaton((SetAsPath) a).stream())
                .collect(Collectors.toSet());

        Set<AutomatonRepresented> origins = new HashSet<>(origins1);
        origins.addAll(origins2);

        AutomatonRepresentedImpl<AsPathRegex> originTrue =
            new AutomatonRepresentedImpl<>(
                AsPathRegex.ALL_AS_PATHS, AsPathRegex.ALL_AS_PATHS.toAutomaton());
        AutomatonAtomicPredicates<AutomatonRepresented> asPathAp =
            new AutomatonAtomicPredicates<>(origins, originTrue);
        ExpressoLogger.log(
            ExpressoLogger.LEVEL.INFO,
            String.format("as path: n_p = %d, n_ap = %d", origins.size(), asPathAp.getNumAtoms()));
      } else {
        Set<AsPathRegex> asPathRegexes = ConfigAtomicPredicates.findAllAsPaths(true);
        asPathAPs = new AutomatonAtomicPredicates<>(asPathRegexes, AsPathRegex.ALL_AS_PATHS);
        ExpressoLogger.log(
            ExpressoLogger.LEVEL.INFO,
            String.format(
                "as path: n_p = %d, n_ap = %d", asPathRegexes.size(), asPathAPs.getNumAtoms()));
        asPathManager = new BddAtomManager<>(asPathAPs);
      }
    }
  }

  public void genConnectedRoute() {
    // generate connected route
    l3Topology
        ._nodes
        .values()
        .forEach(r -> r.virtualRouters.values().forEach(VirtualRouter::genConnectedRoute));
  }

  public void buildOspfTopology() {
    TimeUtil timer = new TimeUtil();
    timer.begin();
    ospfTopology.build(l3Topology);
    timer.end(ExpressoLogger.LEVEL.INFO, "[TOPO]");
  }

  public void computeOspfRoutes() {
    OspfExecutor.execute(ospfTopology);
  }

  public void buildIsisTopology() {
    TimeUtil timer = new TimeUtil();
    timer.begin();
    isisTopology.build(l3Topology);
    timer.end(ExpressoLogger.LEVEL.INFO, "[TOPO]");
  }

  public void computeIsisRoutes() {
    IsisExecutor.execute(isisTopology);
  }

  public void buildBgpTopology() {
    TimeUtil timer = new TimeUtil();
    timer.begin();
    bgpTopology.buildDualRouteReachability(l3Topology);
    timer.end(ExpressoLogger.LEVEL.INFO, "[TOPO]");
  }

  public void computeBgpRoutes() {
    BgpExecutor.execute(bgpTopology);
  }

  private void installRoutes(Consumer<VirtualRouter> consumer) {
    l3Topology._nodes.values().stream()
        .flatMap(router -> router.virtualRouters.values().stream())
        .forEach(consumer);
  }

  public void installOriginalRoutes() {
    installRoutes(VirtualRouter::installOriginalRoutesToGlobalRib);
  }

  public void installIgpRoutes() {
    installRoutes(VirtualRouter::installIgpRoutesToGlobalRib);
  }

  public void activeConditionalStaticRoutes() {
    installRoutes(VirtualRouter::activeConditionalStaticRoutes);
  }

  public void installBgpRoutes() {
    installRoutes(VirtualRouter::installBgpRoutesToGlobalRib);
  }

  public void computeFib() {
    installRoutes(VirtualRouter::computeFib);
  }

  /** Execute control plane computations. */
  public void execute() {
    ExpressoLogger.pushContext("CPE");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    // generate connected route
    genConnectedRoute();
    installOriginalRoutes();

    // igp
    ExpressoLogger.pushContext("OSPF");
    buildOspfTopology();
    computeOspfRoutes();
    ExpressoLogger.popContext();

    ExpressoLogger.pushContext("ISIS");
    buildIsisTopology();
    computeIsisRoutes();
    ExpressoLogger.popContext();

    // install igp routes
    installIgpRoutes();

    // active static routes with next hop ip
    activeConditionalStaticRoutes();

    // compute FIB
    computeFib();

    // bgp
    ExpressoLogger.pushContext("BGP");
    buildBgpTopology();
    computeBgpRoutes();
    ExpressoLogger.popContext();

    // install bgp routes
    installBgpRoutes();

    // compute FIB again
    computeFib();

    timer.end(ExpressoLogger.LEVEL.INFO, "");

    // count number of routes and forwarding rules
    logStatistics(Configuration.LOG_ROUTE_DETAILS);

    ExpressoLogger.popContext();
  }

  private void logStatistics(boolean detail) {
    // count number of routes and forwarding rules
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "num ospf routes: " + Statistics.numOspfRoutes());
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "num isis routes: " + Statistics.numIsisRoutes());
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "num bgp routes: " + Statistics.numBgpRoutes());
    ExpressoLogger.log(
        ExpressoLogger.LEVEL.INFO, "num forwarding rules: " + Statistics.numForwardingRules());
    if (detail) {
      Map<String, Map<String, Integer>> ospfDetails = Statistics.numOspfRoutesInDetail();
      Map<String, Map<String, Integer>> isisDetails = Statistics.numIsisRoutesInDetail();
      Map<String, Map<String, Integer>> bgpDetails = Statistics.numBgpRoutesInDetail();
      Map<String, Map<String, Integer>> forwardingRuleDetails =
          Statistics.numForwardingRulesInDetail();
      ExpressoLogger.log(
          ExpressoLogger.LEVEL.INFO,
          "\nnum ospf routes,\tnum bgp routes,\tnum forwarding rules,\tvrf\n"
              + details(ospfDetails, isisDetails, bgpDetails, forwardingRuleDetails));
    }
  }

  private String details(
      Map<String, Map<String, Integer>> ospfDetails,
      Map<String, Map<String, Integer>> isisDetails,
      Map<String, Map<String, Integer>> bgpDetails,
      Map<String, Map<String, Integer>> forwardingRuleDetails) {
    return ospfDetails.keySet().stream()
        .sorted()
        .flatMap(
            r ->
                ospfDetails.get(r).keySet().stream()
                    .sorted()
                    .map(
                        vr ->
                            String.format(
                                "%d,\t%d,\t%d,\t%d,\tVrf(%s, %s)",
                                ospfDetails.get(r).get(vr),
                                isisDetails.get(r).get(vr),
                                bgpDetails.get(r).get(vr),
                                forwardingRuleDetails.get(r).get(vr),
                                r,
                                vr)))
        .collect(Collectors.joining("\n"));
  }
}
