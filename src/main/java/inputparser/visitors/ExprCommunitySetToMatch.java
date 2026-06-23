package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.CommunityRegex;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchAll;
import datamodel.routepolicy.match.MatchLiteral;
import datamodel.routepolicy.match.MatchOne;
import datamodel.routepolicy.match.MatchTrue;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.routing_policy.expr.CommunityHalvesExpr;
import org.batfish.datamodel.routing_policy.expr.EmptyCommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;

import java.util.stream.Collectors;

import static inputparser.bfvi.RoutePolicyParserHelper.convertCommunity;

public class ExprCommunitySetToMatch implements CommunitySetExprVisitor<Match> {
  final Router router;

  public ExprCommunitySetToMatch(Router router) {
    this.router = router;
  }

  @Override
  public Match visitCommunityHalvesExpr(CommunityHalvesExpr communityHalvesExpr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitCommunityList(CommunityList communityList) {
    // todo check correctness
    return new MatchOne(
        communityList.getLines().stream()
            .map(line -> line.getMatchCondition().accept(this))
            .collect(Collectors.toList()));
  }

  @Override
  public Match visitEmptyCommunitySetExpr(EmptyCommunitySetExpr emptyCommunitySetExpr) {
    return MatchTrue.INSTANCE;
  }

  @Override
  public Match visitLiteralCommunity(LiteralCommunity literalCommunity) {
    return new MatchLiteral(toRegex(literalCommunity.getCommunity()));
  }

  @Override
  public Match visitLiteralCommunityConjunction(
      LiteralCommunityConjunction literalCommunityConjunction) {
    return new MatchAll(
        literalCommunityConjunction.getRequiredCommunities().stream()
            .map(ExprCommunitySetToMatch::toRegex)
            .map(MatchLiteral::new)
            .map(MatchOne::new)
            .collect(Collectors.toList()));
  }

  @Override
  public Match visitLiteralCommunitySet(LiteralCommunitySet literalCommunitySet) {
    return new MatchAll(
        literalCommunitySet.getCommunities().stream()
            .map(ExprCommunitySetToMatch::toRegex)
            .map(MatchLiteral::new)
            .map(MatchOne::new)
            .collect(Collectors.toList()));
  }

  @Override
  public Match visitNamedCommunitySet(NamedCommunitySet namedCommunitySet) {
    // todo
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitRegexCommunitySet(RegexCommunitySet regexCommunitySet) {
    // todo
    throw new UnsupportedOperationException();
  }

  public static CommunityRegex toRegex(org.batfish.datamodel.bgp.community.Community community) {
    return CommunityRegex.from(convertCommunity(community));
  }
}
