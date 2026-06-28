package inputparser.visitors;

import controlplane.network.Router;
import controlplane.route.RoutingProtocol;
import datamodel.Range;
import datamodel.aspath.AsPathRegex;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.routepolicy.RoutePolicy;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchAll;
import datamodel.routepolicy.match.MatchCall;
import datamodel.routepolicy.match.MatchFalse;
import datamodel.routepolicy.match.MatchFilterList;
import datamodel.routepolicy.match.MatchLiteral;
import datamodel.routepolicy.match.MatchOne;
import datamodel.routepolicy.match.MatchTrue;
import datamodel.tag.Tag;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.as_path.MatchAsPath;
import org.batfish.datamodel.routing_policy.communities.MatchCommunities;
import org.batfish.datamodel.routing_policy.expr.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link org.batfish.datamodel.routing_policy.expr.BooleanExpr}s are used as guards in {@link
 * org.batfish.datamodel.routing_policy.statement.If} statements, therefore, we transform them into
 * {@link Match}s.
 */
public class BooleanVisitor implements BooleanExprVisitor<Match, Router> {
  @Override
  public Match visitBooleanExprs(BooleanExprs.StaticBooleanExpr staticBooleanExpr, Router router) {
    switch (staticBooleanExpr.getType()) {
      case True:
      case CallExprContext:
        return MatchTrue.INSTANCE;
      case False:
        return MatchFalse.INSTANCE;
      default:
        throw new UnsupportedOperationException(staticBooleanExpr.getType().toString());
    }
  }

  @Override
  public Match visitCallExpr(CallExpr callExpr, Router router) {
    String calledPolicyName = callExpr.getCalledPolicyName();
    RoutePolicy calledPolicy = router.getRoutePolicy(calledPolicyName);
    return new MatchCall(calledPolicy);
  }

  @Override
  public Match visitConjunction(Conjunction conjunction, Router router) {
    List<Match> conjuncts =
        conjunction.getConjuncts().stream()
            .map(booleanExpr -> booleanExpr.accept(this, router))
            .collect(Collectors.toList());
    return matchAll(conjuncts);
  }

  /**
   * Juniper subroutine chain. Evaluates a route against a series of routing policies in order.
   * Returns a {@link Result} with a boolean value of true if all of the top-level policies accept
   * the route.
   *
   * <p>See more info on chains:
   * https://www.juniper.net/documentation/en_US/junos/topics/concept/policy-routing-policies-chain-evaluation-method.html
   */
  @Override
  public Match visitConjunctionChain(ConjunctionChain conjunctionChain, Router router) {
    List<Match> conjuncts =
        conjunctionChain.getSubroutines().stream()
            .map(booleanExpr -> booleanExpr.accept(this, router))
            .collect(Collectors.toList());
    return matchAll(conjuncts);
  }

  @Override
  public Match visitDisjunction(Disjunction disjunction, Router router) {
    return matchOne(
        disjunction.getDisjuncts().stream()
            .map(booleanExpr -> booleanExpr.accept(this, router))
            .collect(Collectors.toList()));
  }

  /**
   * Juniper subroutine chain. Evaluates a route against a series of routing policies in order.
   * Returns a {@link Result} corresponding to the first policy that matches the route, with a
   * boolean value of true if that policy accepts it or false if that policy rejects it. If none of
   * the policies match the route, returns the result of evaluating the environment's default
   * policy.
   *
   * <p>See more info on chains:
   * https://www.juniper.net/documentation/en_US/junos/topics/concept/policy-routing-policies-chain-evaluation-method.html
   */
  @Override
  public Match visitFirstMatchChain(FirstMatchChain firstMatchChain, Router router) {
    List<Match> matches =
        firstMatchChain.getSubroutines().stream()
            .map(subroutine -> subroutine.accept(this, router))
            .collect(Collectors.toList());
    return matchOne(matches);
  }

  @Override
  public Match visitHasRoute(HasRoute hasRoute, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchAsPath(MatchAsPath matchAsPath, Router router) {
    // HEAD Batfish models AS-path matching as MatchAsPath(AsPathExpr, AsPathMatchExpr). The
    // AsPathExpr is the path under test (InputAsPath for our purposes); the AsPathMatchExpr is the
    // predicate, which we translate into Expresso's Match model.
    return matchAsPath.getAsPathMatchExpr().accept(new AsPathMatchExprToMatch(), router);
  }

  @Override
  public Match visitMatchLegacyAsPath(LegacyMatchAsPath legacyMatchAsPath, Router router) {
    // Legacy AS-path matching survives only over named references (ExplicitAsPathSet was removed).
    AsPathSetExpr asPathSetExpr = legacyMatchAsPath.getExpr();
    if (asPathSetExpr instanceof NamedAsPathSet) {
      String listName = ((NamedAsPathSet) asPathSetExpr).getName();
      return new MatchFilterList(listName, router.getFilterList(FilterType.AS_PATH, listName));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Match visitMatchBgpSessionType(MatchBgpSessionType matchBgpSessionType, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchClusterListLength(
      MatchClusterListLength matchClusterListLength, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchColor(MatchColor matchColor, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchCommunities(MatchCommunities matchCommunities, Router router) {
    // todo
    return matchCommunities
        .getCommunitySetMatchExpr()
        .accept(new CommunitySetMatchExprToMatch(), router);
  }

  @Override
  public Match visitMatchInterface(MatchInterface matchInterface, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchIpv4(MatchIpv4 matchIpv4, Router router) {
    // we only support IPv4 currently, thus always return true
    return MatchTrue.INSTANCE;
  }

  @Override
  public Match visitMatchLocalPreference(MatchLocalPreference matchLocalPreference, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchLocalRouteSourcePrefixLength(
      MatchLocalRouteSourcePrefixLength matchLocalRouteSourcePrefixLength, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchMetric(MatchMetric matchMetric, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchPeerAddress(MatchPeerAddress matchPeerAddress, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchPrefixSet(MatchPrefixSet matchPrefixSet, Router router) {
    PrefixSetExpr prefixSetExpr = matchPrefixSet.getPrefixSet();
    if (prefixSetExpr instanceof ExplicitPrefixSet) {
      List<Match> matches =
          ((ExplicitPrefixSet) prefixSetExpr)
              .getPrefixSpace().getPrefixRanges().stream()
                  .map(
                      prefixRange ->
                          PrefixRange.of(
                              Prefix.of(prefixRange.getPrefix().toString()),
                              new Range<>(
                                  prefixRange.getLengthRange().getStart(),
                                  prefixRange.getLengthRange().getEnd())))
                  .map(MatchLiteral::new)
                  .collect(Collectors.toList());
      return matchOne(matches);
    } else if (prefixSetExpr instanceof NamedPrefixSet) {
      String listName = ((NamedPrefixSet) prefixSetExpr).getName();
      FilterList list = router.getFilterList(FilterType.PREFIX, listName);
      // todo is this correct?
      return list == null ? MatchFalse.INSTANCE : new MatchFilterList(listName, list);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Match visitMatchProcessAsn(MatchProcessAsn matchProcessAsn, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchProtocol(MatchProtocol matchProtocol, Router router) {
    List<Match> matches =
        matchProtocol.getProtocols().stream()
            .map(p -> RoutingProtocol.valueOf(p.toString()))
            .map(MatchLiteral::new)
            .collect(Collectors.toList());
    return matchOne(matches);
  }

  @Override
  public Match visitMatchRouteType(MatchRouteType matchRouteType, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchSourceProtocol(MatchSourceProtocol matchSourceProtocol, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchSourceVrf(MatchSourceVrf matchSourceVrf, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitMatchTag(MatchTag matchTag, Router router) {
    Tag tag = new Tag(matchTag.getTag().accept(new LongVisitor(), router).getKey());
    return new MatchLiteral(tag);
  }

  @Override
  public Match visitNot(Not not, Router router) {
    return not.getExpr().accept(this, router);
  }

  @Override
  public Match visitRouteIsClassful(RouteIsClassful routeIsClassful, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitTrackSucceeded(TrackSucceeded trackSucceeded, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitWithEnvironmentExpr(WithEnvironmentExpr withEnvironmentExpr, Router router) {
    throw new UnsupportedOperationException();
  }

  static Match matchOne(List<Match> matches) {
    if (matches.stream().anyMatch(m -> m instanceof MatchTrue)) {
      return MatchTrue.INSTANCE;
    }
    return matches.size() == 1 ? matches.get(0) : new MatchOne(matches);
  }

  static Match matchAll(List<Match> matches) {
    if (matches.stream().anyMatch(m -> m instanceof MatchFalse)) {
      return MatchFalse.INSTANCE;
    }
    return matches.size() == 1
        ? matches.get(0)
        : new MatchAll(
            matches.stream()
                .map(m -> (MatchOne) (m instanceof MatchOne ? m : new MatchOne(m)))
                .collect(Collectors.toList()));
  }
}
