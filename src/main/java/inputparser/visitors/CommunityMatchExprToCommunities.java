package inputparser.visitors;

import controlplane.network.Router;
import datamodel.community.CommunityRegex;
import org.batfish.datamodel.routing_policy.communities.AllExtendedCommunities;
import org.batfish.datamodel.routing_policy.communities.AllLargeCommunities;
import org.batfish.datamodel.routing_policy.communities.AllStandardCommunities;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprVisitor;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunityNot;
import org.batfish.datamodel.routing_policy.communities.OpaqueExtendedCommunities;
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

public class CommunityMatchExprToCommunities
    implements CommunityMatchExprVisitor<List<CommunityRegex>, Router> {
  @Override
  public List<CommunityRegex> visitAllExtendedCommunities(
      @Nonnull AllExtendedCommunities allExtendedCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitAllLargeCommunities(
      @Nonnull AllLargeCommunities allLargeCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitAllStandardCommunities(
      @Nonnull AllStandardCommunities allStandardCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitCommunityAcl(
      @Nonnull CommunityAcl communityAcl, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitCommunityIn(
      @Nonnull CommunityIn communityIn, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitCommunityIs(
      @Nonnull CommunityIs communityIs, @Nonnull Router router) {
    return list(CommunityRegex.from(convertCommunity(communityIs.getCommunity())));
  }

  @Override
  public List<CommunityRegex> visitCommunityMatchAll(
      @Nonnull CommunityMatchAll communityMatchAll, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitCommunityMatchAny(
      @Nonnull CommunityMatchAny communityMatchAny, @Nonnull Router router) {
    return communityMatchAny.getExprs().stream()
        .flatMap(expr -> expr.accept(this, router).stream())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public List<CommunityRegex> visitCommunityMatchExprReference(
      @Nonnull CommunityMatchExprReference communityMatchExprReference, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitCommunityMatchRegex(
      @Nonnull CommunityMatchRegex communityMatchRegex, @Nonnull Router router) {
    return list(CommunityRegex.from(communityMatchRegex.getRegex()));
  }

  @Override
  public List<CommunityRegex> visitCommunityNot(
      @Nonnull CommunityNot communityNot, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitOpaqueExtendedCommunities(
      @Nonnull OpaqueExtendedCommunities opaqueExtendedCommunities, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitExtendedCommunityGlobalAdministratorHighMatch(
      @Nonnull
          ExtendedCommunityGlobalAdministratorHighMatch
              extendedCommunityGlobalAdministratorHighMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitExtendedCommunityGlobalAdministratorLowMatch(
      @Nonnull
          ExtendedCommunityGlobalAdministratorLowMatch extendedCommunityGlobalAdministratorLowMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitExtendedCommunityGlobalAdministratorMatch(
      @Nonnull ExtendedCommunityGlobalAdministratorMatch extendedCommunityGlobalAdministratorMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitExtendedCommunityLocalAdministratorMatch(
      @Nonnull ExtendedCommunityLocalAdministratorMatch extendedCommunityLocalAdministratorMatch,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitRouteTargetExtendedCommunities(
      @Nonnull RouteTargetExtendedCommunities routeTargetExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitSiteOfOriginExtendedCommunities(
      @Nonnull SiteOfOriginExtendedCommunities siteOfOriginExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitStandardCommunityHighMatch(
      @Nonnull StandardCommunityHighMatch standardCommunityHighMatch, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitStandardCommunityLowMatch(
      @Nonnull StandardCommunityLowMatch standardCommunityLowMatch, @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CommunityRegex> visitVpnDistinguisherExtendedCommunities(
      @Nonnull VpnDistinguisherExtendedCommunities vpnDistinguisherExtendedCommunities,
      @Nonnull Router router) {
    throw new UnsupportedOperationException();
  }
}
