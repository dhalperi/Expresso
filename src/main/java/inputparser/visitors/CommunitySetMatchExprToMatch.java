package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.CommunityRegex;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchLiteral;
import datamodel.routepolicy.match.MatchNot;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAcl;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExprVisitor;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetNot;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.HasSize;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static inputparser.visitors.BooleanVisitor.matchAll;
import static inputparser.visitors.BooleanVisitor.matchOne;

public class CommunitySetMatchExprToMatch implements CommunitySetMatchExprVisitor<Match, Router> {
  @Override
  public Match visitCommunitySetAcl(
      @Nonnull CommunitySetAcl communitySetAcl, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Match visitCommunitySetMatchAll(
      @Nonnull CommunitySetMatchAll communitySetMatchAll, @Nonnull Router router) {
    List<Match> matches =
        communitySetMatchAll.getExprs().stream()
            .map(expr -> expr.accept(this, router))
            .collect(Collectors.toList());
    return matchAll(matches);
  }

  @Override
  public Match visitCommunitySetMatchAny(
      @Nonnull CommunitySetMatchAny communitySetMatchAny, @Nonnull Router router) {
    List<Match> matches =
        communitySetMatchAny.getExprs().stream()
            .map(expr -> expr.accept(this, router))
            .collect(Collectors.toList());
    return matchOne(matches);
  }

  @Override
  public Match visitCommunitySetMatchExprReference(
      @Nonnull CommunitySetMatchExprReference communitySetMatchExprReference, Router router) {
    CommunitySetMatchExpr expr =
        router.getCommunitySetMatchExpr().get(communitySetMatchExprReference.getName());
    return expr.accept(this, router);
  }

  @Override
  public Match visitCommunitySetMatchRegex(
      @Nonnull CommunitySetMatchRegex communitySetMatchRegex, @Nonnull Router router) {
    CommunityRegex regex = CommunityRegex.from(communitySetMatchRegex.getRegex());
    return new MatchLiteral(regex);
  }

  @Override
  public Match visitCommunitySetNot(
      @Nonnull CommunitySetNot communitySetNot, @Nonnull Router router) {
    Match match = communitySetNot.getExpr().accept(this, router);
    return new MatchNot(match);
  }

  @Override
  public Match visitHasCommunity(@Nonnull HasCommunity hasCommunity, @Nonnull Router router) {
    List<CommunityRegex> regexes =
        hasCommunity.getExpr().accept(new CommunityMatchExprToCommunities(), router);
    return matchOne(regexes.stream().map(MatchLiteral::new).collect(Collectors.toList()));
  }

  @Override
  public Match visitHasSize(@Nonnull HasSize hasSize, @Nonnull Router router) {
    // Matching on the number of communities in a route is not modeled by Expresso.
    throw new UnsupportedOperationException();
  }
}
