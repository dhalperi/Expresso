package atomic;

import atomic.automaton.AutomatonAtomicPredicates;
import atomic.automaton.AutomatonRepresentedImpl;
import atomic.bdd.BddAtomicPredicates;
import controlplane.network.Router;
import datamodel.CanBeMatched;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.filterlist.Mode;
import datamodel.ipv4.PrefixRange;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetAsPath;
import dk.brics.automaton.Automaton;
import javafx.util.Pair;
import main.Controller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class computes the community-regex, AS-path-regex and prefix-range-bdd atomic predicates for
 * all router configurations.
 */
public class ConfigAtomicPredicates {
  /**
   * Atomic predicates for community literals and regexes that appear in the given configurations.
   */
  AutomatonAtomicPredicates<CommunityRegex> _communityAtomicPredicates;

  /** Atomic predicates for the AS-path regexes that appear in the given configurations. */
  AutomatonAtomicPredicates<AsPathRegex> _asPathAtomicPredicates;

  /** Atomic predicates for the prefix ranges that appear in the given configurations. */
  BddAtomicPredicates<PrefixRange> _prefixRangeAtomicPredicates;

  public ConfigAtomicPredicates() {
    _communityAtomicPredicates =
        new AutomatonAtomicPredicates<>(
            findAllCommunities(true), CommunityRegex.ALL_STANDARD_COMMUNITIES);
    _asPathAtomicPredicates =
        new AutomatonAtomicPredicates<>(findAllAsPaths(true), AsPathRegex.ALL_AS_PATHS);
    _prefixRangeAtomicPredicates = new BddAtomicPredicates<>(findAllPrefixRanges(true));
  }

  /**
   * Collect up all community literals and regexes in the configurations by walking over all route
   * policies.
   *
   * @return a set of all community regexes that appear.
   */
  public static Set<CommunityRegex> findAllCommunities(boolean used) {
    Set<CommunityRegex> comms = new HashSet<>();
    for (Router router : Controller.cpExecutor.l3Topology.getNodes().values()) {
      router.getRoutePolicies().values().stream()
          .filter(rp -> !used || rp.isUsed())
          .forEach(routePolicy -> comms.addAll(routePolicy.collectCommunityRegexes()));
    }
    return comms;
  }

  /**
   * Collect up all AS-path regexes that appear in the configurations by walking over all route
   * policies.
   *
   * @return a set of all AS-path regexes that appear.
   */
  public static Set<AsPathRegex> findAllAsPaths(boolean used) {
    Set<AsPathRegex> asPathRegexes = new HashSet<>();
    for (Router router : Controller.cpExecutor.l3Topology.getNodes().values()) {
      router.getRoutePolicies().values().stream()
          .filter(rp -> !used || rp.isUsed())
          .forEach(routePolicy -> asPathRegexes.addAll(routePolicy.collectAsPathRegexes()));
    }
    return asPathRegexes;
  }

  /**
   * Collect up all prefix ranges that appear in the configurations by walking over all route
   * policies.
   *
   * @return a set of all prefix ranges that appear.
   */
  public static Set<PrefixRange> findAllPrefixRanges(boolean used) {
    Set<PrefixRange> prefixRanges =
        Controller.cpExecutor.l3Topology.getPrefixOwners().keys().stream()
            .map(PrefixRange::of)
            .collect(Collectors.toSet());
    for (Router router : Controller.cpExecutor.l3Topology.getNodes().values()) {
      router.getRoutePolicies().values().stream()
          .filter(rp -> !used || rp.isUsed())
          .forEach(routePolicy -> prefixRanges.addAll(routePolicy.collectPrefixRanges()));
      router.getFilterLists().values().stream()
          .filter(fl -> !used || fl.isUsed())
          .forEach(filterList -> prefixRanges.addAll(filterList.collectPrefixRanges()));
    }
    return prefixRanges;
  }

  public AutomatonAtomicPredicates<CommunityRegex> getCommunityAtomicPredicates() {
    return _communityAtomicPredicates;
  }

  public AutomatonAtomicPredicates<AsPathRegex> getAsPathAtomicPredicates() {
    return _asPathAtomicPredicates;
  }

  public BddAtomicPredicates<PrefixRange> getPrefixRangeAtomicPredicates() {
    return _prefixRangeAtomicPredicates;
  }

  public static Set<FilterList> findAllFilterListsOfType(FilterType type, boolean used) {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .map(n -> n.getFilterLists().rowMap().get(type))
        .filter(Objects::nonNull)
        .flatMap(map -> map.values().stream())
        .filter(fl -> !used || fl.isUsed())
        .collect(Collectors.toSet());
  }

  public static <A extends Action> Set<Action> findAllRoutePolicyActionsOfClass(
      Class<A> clz, boolean used) {
    return Controller.cpExecutor.l3Topology.getNodes().values().stream()
        .flatMap(n -> n.getRoutePolicies().values().stream())
        .filter(rp -> !used || rp.isUsed())
        .flatMap(rp -> rp.getNodes().values().stream())
        .flatMap(n -> n.getActions().stream())
        .filter(a -> a.getClass() == clz)
        .collect(Collectors.toSet());
  }

  public static AutomatonRepresentedImpl<FilterList> convertAsPathListToAutomaton(
      FilterList communityFilter) {
    Automaton permitted = Automaton.makeEmpty();
    Automaton denied = Automaton.makeEmpty();
    for (Pair<CanBeMatched, Mode> line : communityFilter.getLines()) {
      if (line.getKey() instanceof AsPathRegex) {
        Automaton cur = ((AsPathRegex) line.getKey()).toAutomaton();
        if (line.getValue() == Mode.PERMIT) {
          // union not already denied
          permitted = permitted.union(cur.minus(denied));
        } else {
          // union not already permitted
          denied = denied.union(cur.minus(permitted));
        }
      }
    }
    return new AutomatonRepresentedImpl<>(communityFilter, permitted);
  }

  public static List<AutomatonRepresentedImpl<AsPathRegex>> convertSetAsPathToAutomaton(
      SetAsPath setAsPath) {
    AsPathRegex asPathRegex = setAsPath.getAsPathRegex();
    return Collections.singletonList(
        new AutomatonRepresentedImpl<>(asPathRegex, asPathRegex.toAutomaton()));
  }
}
