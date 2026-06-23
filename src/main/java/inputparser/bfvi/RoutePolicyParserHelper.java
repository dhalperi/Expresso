package inputparser.bfvi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Router;
import datamodel.community.Community;
import inputparser.CommunityParser;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.expr.AsPathListExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitAs;
import org.batfish.datamodel.routing_policy.expr.LiteralAsList;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import util.JsonUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

public class RoutePolicyParserHelper {
  public static void deserializeCommunityExprs(Router router, JsonObject root) {
    JsonObject jsonCommunityMatchExprs = root.getAsJsonObject("communityMatchExprs");
    SortedMap<String, CommunityMatchExpr> communityMatchExprs =
        deserializeExprs(jsonCommunityMatchExprs, CommunityMatchExpr.class);
    router.setCommunityMatchExpr(communityMatchExprs);

    JsonObject jsonCommunitySetMatchExprs = root.getAsJsonObject("communitySetMatchExprs");
    SortedMap<String, CommunitySetMatchExpr> communitySetMatchExprs =
        deserializeExprs(jsonCommunitySetMatchExprs, CommunitySetMatchExpr.class);
    router.setCommunitySetMatchExpr(communitySetMatchExprs);

    JsonObject jsonCommunitySets = root.getAsJsonObject("communitySets");
    SortedMap<String, CommunitySet> communitySets =
        deserializeExprs(jsonCommunitySets, CommunitySet.class);
    router.setCommunitySets(communitySets);
  }

  private static <E> SortedMap<String, E> deserializeExprs(JsonObject jsonObject, Class<E> clz) {
    ImmutableSortedMap.Builder<String, E> map = ImmutableSortedMap.naturalOrder();
    for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
      try {
        E expr = JsonUtil.mapper.readValue(e.getValue().toString(), clz);
        map.put(e.getKey(), expr);
      } catch (JsonProcessingException ignored) {
      }
    }
    return map.build();
  }

  /** Deserialize a JSON object to a {@link org.batfish.datamodel.routing_policy.RoutingPolicy}. */
  public static RoutingPolicy deserializeRoutingPolicy(JsonObject jsonRoutePolicy)
      throws JsonProcessingException {
    return JsonUtil.mapper.readValue(jsonRoutePolicy.toString(), RoutingPolicy.class);
  }

  static boolean isStaticGuard(BooleanExpr guard) {
    return guard instanceof Conjunction
        && ((Conjunction) guard)
            .getConjuncts().stream()
                .allMatch(
                    c ->
                        (c instanceof MatchProtocol
                                && ((MatchProtocol) c)
                                    .getProtocols().stream()
                                        .allMatch(p -> p == RoutingProtocol.STATIC))
                            || (c instanceof MatchPrefixSet
                                && ((MatchPrefixSet) c).getPrefix() instanceof DestinationNetwork));
  }

  public enum SetType {
    NONE,
    INCREMENT,
    DECREMENT
  }

  public static final If ACCEPT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          list(Statements.ReturnTrue.toStaticStatement()),
          list(Statements.ExitAccept.toStaticStatement()));
  public static final If REJECT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          list(Statements.ReturnFalse.toStaticStatement()),
          list(Statements.ExitReject.toStaticStatement()));
  public static final If RETURN =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          Collections.emptyList(),
          list(Statements.Return.toStaticStatement()));
  public static final Statement FALL_THROUGH = Statements.FallThrough.toStaticStatement();

  public static <T> List<T> list(T t) {
    return ImmutableList.of(t);
  }

  public static List<Long> getAsPath(AsPathListExpr asPathListExpr) {
    if (asPathListExpr instanceof LiteralAsList) {
      LiteralAsList literalAsList = (LiteralAsList) asPathListExpr;
      if (literalAsList.getList().stream().allMatch(asExpr -> asExpr instanceof ExplicitAs)) {
        return literalAsList.getList().stream()
            .map(asExpr -> ((ExplicitAs) asExpr).getAs())
            .collect(Collectors.toList());
      }
    }
    throw new UnsupportedOperationException();
  }

  /** Convert a {@link org.batfish.datamodel.bgp.community.Community} to {@link Community}. */
  public static Community convertCommunity(org.batfish.datamodel.bgp.community.Community comm) {
    return CommunityParser.parseCommunity(comm.toString());
  }

  /**
   * Convert a collection of {@link org.batfish.datamodel.bgp.community.Community}s to a list of
   * {@link Community}s.
   */
  public static List<Community> convertCommunities(
      Collection<org.batfish.datamodel.bgp.community.Community> comms) {
    return comms.stream()
        .map(RoutePolicyParserHelper::convertCommunity)
        .collect(Collectors.toList());
  }
}
