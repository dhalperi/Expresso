package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.CommunityRegex;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.RemoveMatchedCommunities;
import datamodel.routepolicy.action.SetCommunity;
import org.batfish.datamodel.routing_policy.communities.AllExtendedCommunities;
import org.batfish.datamodel.routing_policy.communities.AllLargeCommunities;
import org.batfish.datamodel.routing_policy.communities.AllStandardCommunities;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprVisitor;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunityNot;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorHighMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorLowMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityLocalAdministratorMatch;
import org.batfish.datamodel.routing_policy.communities.RouteTargetExtendedCommunities;
import org.batfish.datamodel.routing_policy.communities.SiteOfOriginExtendedCommunities;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighMatch;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityLowMatch;
import org.batfish.datamodel.routing_policy.communities.VpnDistinguisherExtendedCommunities;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static inputparser.bfvi.RoutePolicyParserHelper.convertCommunity;
import static inputparser.bfvi.RoutePolicyParserHelper.list;

public class CommunityMatchExprToAction implements CommunityMatchExprVisitor<Action, Router> {
  @Override
  public Action visitAllExtendedCommunities(
      @Nonnull AllExtendedCommunities allExtendedCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitAllLargeCommunities(
      @Nonnull AllLargeCommunities allLargeCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitAllStandardCommunities(
      @Nonnull AllStandardCommunities allStandardCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitCommunityAcl(@Nonnull CommunityAcl communityAcl, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitCommunityIn(@Nonnull CommunityIn communityIn, @Nonnull Router router) {
    return communityIn.getCommunitySetExpr().accept(new CommunitySetToAction(), router);
  }

  @Override
  public Action visitCommunityIs(@Nonnull CommunityIs communityIs, @Nonnull Router router) {
    return SetCommunity.decrease(convertCommunity(communityIs.getCommunity()));
  }

  @Override
  public Action visitCommunityMatchAll(
      @Nonnull CommunityMatchAll communityMatchAll, @Nonnull Router router) {
    List<CommunityRegex> regexes =
        communityMatchAll.getExprs().stream()
            .flatMap(expr -> expr.accept(this, router).collectCommunityRegexes().stream())
            .collect(Collectors.toList());
    return new RemoveMatchedCommunities(true, regexes);
  }

  @Override
  public Action visitCommunityMatchAny(
      @Nonnull CommunityMatchAny communityMatchAny, @Nonnull Router router) {
    List<CommunityRegex> regexes =
        communityMatchAny.getExprs().stream()
            .flatMap(expr -> expr.accept(this, router).collectCommunityRegexes().stream())
            .collect(Collectors.toList());
    return new RemoveMatchedCommunities(false, regexes);
  }

  @Override
  public Action visitCommunityMatchExprReference(
      @Nonnull CommunityMatchExprReference communityMatchExprReference, @Nonnull Router router) {
    CommunityMatchExpr expr =
        router.getCommunityMatchExpr().get(communityMatchExprReference.getName());
    return expr.accept(this, router);
  }

  @Override
  public Action visitCommunityMatchRegex(
      @Nonnull CommunityMatchRegex communityMatchRegex, @Nonnull Router router) {
    return new RemoveMatchedCommunities(
        true, list(CommunityRegex.from(communityMatchRegex.getRegex())));
  }

  @Override
  public Action visitCommunityNot(@Nonnull CommunityNot communityNot, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitExtendedCommunityGlobalAdministratorHighMatch(
      @Nonnull
          ExtendedCommunityGlobalAdministratorHighMatch
              extendedCommunityGlobalAdministratorHighMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitExtendedCommunityGlobalAdministratorLowMatch(
      @Nonnull
          ExtendedCommunityGlobalAdministratorLowMatch extendedCommunityGlobalAdministratorLowMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitExtendedCommunityGlobalAdministratorMatch(
      @Nonnull ExtendedCommunityGlobalAdministratorMatch extendedCommunityGlobalAdministratorMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitExtendedCommunityLocalAdministratorMatch(
      @Nonnull ExtendedCommunityLocalAdministratorMatch extendedCommunityLocalAdministratorMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitRouteTargetExtendedCommunities(
      @Nonnull RouteTargetExtendedCommunities routeTargetExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitSiteOfOriginExtendedCommunities(
      @Nonnull SiteOfOriginExtendedCommunities siteOfOriginExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitStandardCommunityHighMatch(
      @Nonnull StandardCommunityHighMatch standardCommunityHighMatch, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitStandardCommunityLowMatch(
      @Nonnull StandardCommunityLowMatch standardCommunityLowMatch, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitVpnDistinguisherExtendedCommunities(
      @Nonnull VpnDistinguisherExtendedCommunities vpnDistinguisherExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }
}
