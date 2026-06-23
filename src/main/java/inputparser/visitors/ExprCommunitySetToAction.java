package inputparser.visitors;

import controlplane.network.Router;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetCommunity;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.routing_policy.expr.CommunityHalvesExpr;
import org.batfish.datamodel.routing_policy.expr.EmptyCommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;

import java.util.Collections;

import static inputparser.bfvi.RoutePolicyParserHelper.convertCommunities;
import static inputparser.bfvi.RoutePolicyParserHelper.convertCommunity;

public class ExprCommunitySetToAction implements CommunitySetExprVisitor<Action> {
  final Router router;

  public ExprCommunitySetToAction(Router router) {
    this.router = router;
  }

  @Override
  public Action visitCommunityHalvesExpr(CommunityHalvesExpr communityHalvesExpr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitCommunityList(CommunityList communityList) {
    // todo
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitEmptyCommunitySetExpr(EmptyCommunitySetExpr emptyCommunitySetExpr) {
    return SetCommunity.replace(Collections.emptyList());
  }

  @Override
  public Action visitLiteralCommunity(LiteralCommunity literalCommunity) {
    return SetCommunity.replace(convertCommunity(literalCommunity.getCommunity()));
  }

  @Override
  public Action visitLiteralCommunityConjunction(
      LiteralCommunityConjunction literalCommunityConjunction) {
    return SetCommunity.replace(
        convertCommunities(literalCommunityConjunction.getRequiredCommunities()));
  }

  @Override
  public Action visitLiteralCommunitySet(LiteralCommunitySet literalCommunitySet) {
    return SetCommunity.replace(convertCommunities(literalCommunitySet.getCommunities()));
  }

  @Override
  public Action visitNamedCommunitySet(NamedCommunitySet namedCommunitySet) {
    // todo
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitRegexCommunitySet(RegexCommunitySet regexCommunitySet) {
    // todo
    throw new UnsupportedOperationException();
  }
}
