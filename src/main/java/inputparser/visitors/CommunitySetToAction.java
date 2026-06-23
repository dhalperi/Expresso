package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.Community;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetCommunity;
import org.batfish.datamodel.routing_policy.communities.CommunityExprsSet;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetUnion;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static inputparser.bfvi.RoutePolicyParserHelper.convertCommunities;

public class CommunitySetToAction
    implements org.batfish.datamodel.routing_policy.communities.CommunitySetExprVisitor<
        Action, Router> {
  @Override
  public Action visitCommunityExprsSet(
      CommunityExprsSet communityExprsSet, @Nonnull Router router) {
    return SetCommunity.replace(
        communityExprsSet.getExprs().stream()
            .map(expr -> expr.accept(new CommunitiesCommunityVisitor(), router))
            .flatMap(List::stream)
            .collect(Collectors.toList()));
  }

  private static final HashSet<String> difference = new HashSet<>();

  @Override
  public Action visitCommunitySetDifference(
      CommunitySetDifference communitySetDifference, @Nonnull Router router) {
    if (communitySetDifference.getInitial() instanceof InputCommunities) {
      return communitySetDifference
          .getRemovalCriterion()
          .accept(new CommunityMatchExprToAction(), router);
    } else {
      String str =
          String.format(
              "%s %s",
              communitySetDifference.getInitial().getClass().getName(),
              communitySetDifference.getRemovalCriterion().getClass().getName());
      if (!difference.contains(str)) {
        System.out.println("Visiting CommunitySetDifference " + str);
        difference.add(str);
      }
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Action visitCommunitySetExprReference(
      CommunitySetExprReference communitySetExprReference, Router router) {
    // todo is this the best way?
    CommunitySetExpr expr = router.getCommunitySetExprs().get(communitySetExprReference.getName());
    return expr.accept(this, router);
  }

  @Override
  public Action visitCommunitySetReference(
      CommunitySetReference communitySetReference, Router router) {
    // todo is this the best way?
    CommunitySet communitySet = router.getCommunitySets().get(communitySetReference.getName());
    return SetCommunity.replace(convertCommunities(communitySet.getCommunities()));
  }

  @Override
  public Action visitCommunitySetUnion(
      @Nonnull CommunitySetUnion communitySetUnion, @Nonnull Router router) {
    // todo check correctness
    boolean incremental =
        communitySetUnion.getExprs().stream().anyMatch(expr -> expr instanceof InputCommunities);
    List<Community> communities =
        communitySetUnion.getExprs().stream()
            .flatMap(expr -> ((SetCommunity) expr.accept(this, router)).getCommunities().stream())
            .distinct()
            .collect(Collectors.toList());
    return incremental ? SetCommunity.increase(communities) : SetCommunity.replace(communities);
  }

  @Override
  public Action visitInputCommunities(
      @Nullable InputCommunities inputCommunities, @Nullable Router router) {
    return SetCommunity.increase();
  }

  @Override
  public Action visitLiteralCommunitySet(
      LiteralCommunitySet literalCommunitySet, @Nullable Router router) {
    return SetCommunity.replace(
        convertCommunities(literalCommunitySet.getCommunitySet().getCommunities()));
  }
}
