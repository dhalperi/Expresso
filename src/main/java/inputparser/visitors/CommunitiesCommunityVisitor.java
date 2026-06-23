package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.Community;
import datamodel.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.communities.CommunityExprVisitor;
import org.batfish.datamodel.routing_policy.communities.RouteTargetExtendedCommunityExpr;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighLowExprs;

import javax.annotation.Nonnull;
import java.util.List;

import static inputparser.bfvi.RoutePolicyParserHelper.list;

public class CommunitiesCommunityVisitor implements CommunityExprVisitor<List<Community>, Router> {
  @Override
  public List<Community> visitRouteTargetExtendedCommunityExpr(
      @Nonnull RouteTargetExtendedCommunityExpr routeTargetExtendedCommunityExpr,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Community> visitStandardCommunityHighLowExprs(
      StandardCommunityHighLowExprs standardCommunityHighLowExprs, @Nonnull Router router) {
    int high = standardCommunityHighLowExprs.getHighExpr().accept(new IntVisitor(), router);
    int low = standardCommunityHighLowExprs.getLowExpr().accept(new IntVisitor(), router);
    return list(StandardCommunity.of(high, low));
  }
}
