package datamodel.routepolicy;

import com.google.common.collect.ImmutableList;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.collector.Collector;
import datamodel.community.CommunityRegex;
import datamodel.filterlist.Mode;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilter;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.RemovePrivateAs;
import datamodel.routepolicy.match.AllowAsLoop;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchAll;
import datamodel.routepolicy.match.MatchFalse;
import datamodel.routepolicy.match.MatchOne;
import datamodel.routepolicy.match.MatchTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class Node implements Collector, RouteFilter {
  public static Node PERMIT_ALL =
      new Node(Mode.PERMIT, MatchTrue.INSTANCE, Collections.emptyList());
  public static Node DENY_ALL = new Node(Mode.DENY, MatchFalse.INSTANCE, Collections.emptyList());
  public static Node REMOVE_PRIVATE_AS =
      new Node(
          Mode.PERMIT, MatchTrue.INSTANCE, Collections.singletonList(RemovePrivateAs.INSTANCE));
  public static Node ALLOW_AS_LOOP =
      new Node(Mode.PERMIT, AllowAsLoop.INSTANCE, Collections.emptyList());

  final Mode mode;
  /**
   * Why we use {@link MatchAll} here? <br>
   * From <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1000017273/c2e927da/optional-configuring-an-if-match-clause">huawei
   * doc</>: <br>
   * If no if-match clause is configured for a node in a routing policy, all routes match in this
   * node. If one or more if-match clauses are configured in a node, the relationship between the
   * clauses is "AND". This means that routes match this node only when they match all the if-match
   * clauses in this node. When multiple if-match as-path-filter, if-match community-filter,
   * if-match interface, or if-match route-type clauses are configured, the relationship between the
   * clauses is "OR". The relationship of the five clauses is "AND", and the relationship between
   * the five clauses and other clauses is also "AND". If multiple if-match as-path-filter clauses
   * are configured in a node, the relationship of these clauses is "OR", and the relationship
   * between these clauses and other if-match clauses is "AND".
   */
  final MatchAll matches;

  final List<Action> actions;

  public Node(Mode mode, MatchAll matchAll, List<Action> actions) {
    this.mode = mode;
    this.matches = matchAll;
    this.actions = ImmutableList.copyOf(firstNonNull(actions, Collections.emptyList()));
  }

  public Node(Mode mode, Match match, List<Action> actions) {
    this.mode = mode;
    this.matches =
        match instanceof MatchAll
            ? (MatchAll) match
            : new MatchAll(match instanceof MatchOne ? (MatchOne) match : new MatchOne(match));
    this.actions = ImmutableList.copyOf(firstNonNull(actions, Collections.emptyList()));
  }

  public MatchAll getMatchAll() {
    return matches;
  }

  public List<Action> getActions() {
    return actions;
  }

  /**
   * @return {@link RouteFilterResult} the route will be split into two parts (permitted and
   *     denied).
   *     <p>Permitted: the part that matches this node's match condition. Denied: the part that
   *     doesn't match this node's match condition.
   *     <p>If the mode of this node is "permit" ({@link Mode#PERMIT}, actions will be done on the
   *     permitted part. If the mode of this node is "deny" ({@link Mode#DENY}, actions will be done
   *     on the denied part.
   */
  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = matches.filter(route, environment);
    if (mode == Mode.PERMIT) {
      List<R> permittedRoutes =
          result.getPermitted().getAllRoutes().stream()
              .peek(permitted -> actions.forEach(action -> action.act(permitted, environment)))
              .collect(Collectors.toList());
      result.setPermitted(new RouteSet<>(permittedRoutes));
    } else {
      List<R> deniedRoutes =
          result.getDenied().getAllRoutes().stream()
              .peek(denied -> actions.forEach(action -> action.act(denied, environment)))
              .collect(Collectors.toList());
      result.setDenied(new RouteSet<>(deniedRoutes));
    }
    return result;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return matches.collectPrefixRanges();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    Set<CommunityRegex> regexes = matches.collectCommunityRegexes();
    actions.forEach(action -> regexes.addAll(action.collectCommunityRegexes()));
    return regexes;
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    Set<AsPathRegex> regexes = matches.collectAsPathRegexes();
    actions.forEach(action -> regexes.addAll(action.collectAsPathRegexes()));
    return regexes;
  }
}
