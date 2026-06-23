package controlplane.process.bgp.routeaggregation;

import controlplane.route.BgpRoute;
import controlplane.route.Route;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.filterlist.Mode;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.routepolicy.Node;
import datamodel.routepolicy.RoutePolicy;
import datamodel.routepolicy.match.MatchLiteral;
import util.BddUtil;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static main.Controller.bddManager;

/**
 * <a
 * href="https://support.huawei.com/enterprise/en/doc/EDOC1000128405/a5159bf2/aggregate-bgp">huawei-doc</a>
 */
public class RouteAggregation {
  Prefix prefix;
  RoutePolicy moreSpecificThan;
  /**
   * This parameter is used to create an aggregated route whose AS_Path attribute contains AS_Path
   * information of specific routes. I.e., add all ASNs in specific routes' AS_Paths into a AS set.
   * Exercise caution when using this parameter if many AS_Path attributes need to be aggregated
   * because frequent changes in routes may cause route flapping.
   */
  boolean asSet;
  /**
   * This parameter is used to suppress the advertisement of specific routes. After
   * detail-suppressed is configured, only aggregated routes are advertised. Aggregated routes carry
   * the atomic-aggregate attribute, not the community attributes of specific routes.
   */
  boolean detailSuppressed;
  /**
   * This parameter attribute-policy is used to set attributes for a aggregated route. If the
   * AS_Path attribute is set in the policy using the apply as-path command and as-set is set in the
   * aggregate command, the AS_Path attribute in the policy does not take effect.
   */
  RoutePolicy attributePolicy;
  /** After this parameter is used, only the routes matching route-policy are aggregated. */
  RoutePolicy originPolicy;
  /**
   * This parameter is used to suppress the advertisement of specified routes. The if-match clause
   * of route-policy can be used to filter the routes to be suppressed. This means that only the
   * routes matching the policy will be suppressed, and the other routes will still be advertised.
   */
  RoutePolicy suppressPolicy;

  public RouteAggregation(
      Prefix prefix,
      boolean asSet,
      boolean detailSuppressed,
      RoutePolicy attributePolicy,
      RoutePolicy originPolicy,
      RoutePolicy suppressPolicy) {
    this.prefix = prefix;
    this.asSet = asSet;
    this.detailSuppressed = detailSuppressed;
    this.attributePolicy = attributePolicy;
    this.originPolicy = originPolicy;
    this.suppressPolicy = suppressPolicy;
    this.init();
  }

  private void init() {
    moreSpecificThan =
        new RoutePolicy(
            "MoreSpecificThan",
            new Node(Mode.PERMIT, new MatchLiteral(PrefixRange.moreSpecificThan(prefix)), null));
    attributePolicy = attributePolicy == null ? RoutePolicy.PERMIT_ALL : attributePolicy;
    originPolicy = originPolicy == null ? RoutePolicy.PERMIT_ALL : originPolicy;
    suppressPolicy =
        detailSuppressed
            ? (suppressPolicy == null ? RoutePolicy.DENY_ALL : suppressPolicy)
            : RoutePolicy.PERMIT_ALL;
  }

  public RouteAggregationResult process(Collection<BgpRoute> routes) {
    RouteAggregationResult result = new RouteAggregationResult(prefix);

    // find all routes whose prefix is more specific
    for (BgpRoute route : routes) {
      RouteFilterResult<BgpRoute> moreSpecificResult =
          moreSpecificThan.filter(
              route,
              new RouteFilterEnvironment.Builder<BgpRoute>()
                  .setIn(true)
                  .setLocalConfig(null)
                  .build());
      result.others.addAll(moreSpecificResult.getDenied());
      result.specifics.addAll(moreSpecificResult.getPermitted());
    }

    // find all specific routes that are aggregated
    for (BgpRoute specificRoute : result.specifics.getAllRoutes()) {
      RouteFilterResult<BgpRoute> originResult =
          originPolicy.filter(
              specificRoute,
              new RouteFilterEnvironment.Builder<BgpRoute>()
                  .setIn(true)
                  .setLocalConfig(null)
                  .build());
      result.others.addAll(originResult.getDenied());
      result.origins.addAll(originResult.getPermitted());
    }

    // find all aggregated routes that are suppressed/not suppressed
    for (BgpRoute originRoute : result.origins.getAllRoutes()) {
      RouteFilterResult<BgpRoute> suppressResult =
          suppressPolicy.filter(
              originRoute,
              new RouteFilterEnvironment.Builder<BgpRoute>()
                  .setIn(true)
                  .setLocalConfig(null)
                  .build());
      result.suppressed.addAll(suppressResult.getPermitted());
      result.notSuppressed.addAll(suppressResult.getDenied());
    }

    if (asSet) {
      // todo
    } else {
      // generate aggregation routes
      int igpEnvc = collect(result, BgpRoute.OriginType.IGP);
      int egpEnvc = collect(result, BgpRoute.OriginType.EGP);
      int incompleteEnvc = collect(result, BgpRoute.OriginType.INCOMPLETE);
      generate(result, BgpRoute.OriginType.IGP, igpEnvc);
      generate(result, BgpRoute.OriginType.EGP, bddManager.minus(egpEnvc, igpEnvc));
      generate(
          result,
          BgpRoute.OriginType.INCOMPLETE,
          bddManager.minus(incompleteEnvc, bddManager.and(igpEnvc, egpEnvc)));
    }

    return result;
  }

  private int collect(RouteAggregationResult result, BgpRoute.OriginType origin) {
    Collection<BgpRoute> routes =
        Stream.concat(
                result.suppressed.getAllRoutes().stream(),
                result.notSuppressed.getAllRoutes().stream())
            .filter(bgpRoute -> bgpRoute.getOrigin() == origin)
            .collect(Collectors.toList());
    //        return BddUtil.orInBatch(
    //                bddManager.getBDD(),
    //                routes
    //                        .stream()
    //                        .flatMap(bgpRoute ->
    //
    // bddManager.getBddPrefixWrapper().splitPrefixRangeEnvc(bgpRoute.getPrefixesBdd()).values().stream())
    //                        .collect(Collectors.toList())
    //        );
    return bddManager
        .getBddEnvcWrapper()
        .transformCpEnvc2DpEnvcMoreSpecific(
            BddUtil.orInBatch(
                bddManager.getBDD(),
                routes.stream().map(Route::getPrefixesBdd).collect(Collectors.toList())),
            prefix.getPreLength());
  }

  private void generate(RouteAggregationResult result, BgpRoute.OriginType origin, int envc) {
    if (envc != 0) {
      BgpRouteBuilder builder =
          new BgpRouteBuilder()
              .setPrefixesBdd(bddManager.and(result.prefixBdd, envc))
              .setType(BgpRoute.BgpRouteType.AGGREGATE)
              .setOrigin(origin);
      RouteFilterResult<BgpRoute> tmp =
          attributePolicy.filter(
              builder.build(),
              new RouteFilterEnvironment.Builder<BgpRoute>()
                  .setIn(true)
                  .setLocalConfig(null)
                  .build());
      result.aggrs.addAll(tmp.getPermitted());
    }
  }
}
