package inputparser.bfvi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Router;
import datamodel.routepolicy.NestedRoutePolicy;
import datamodel.routepolicy.RoutePolicy;
import inputparser.visitors.StmtVisitor;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.*;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static inputparser.bfvi.RoutePolicyParserHelper.isStaticGuard;
import static inputparser.bfvi.RoutePolicyParserHelper.deserializeCommunityExprs;
import static inputparser.bfvi.RoutePolicyParserHelper.deserializeRoutingPolicy;

/**
 * Parse {@link RoutePolicy} from JSON configurations. Specifically, first deserialize a {@link
 * RoutingPolicy} (i.e., the model for routing policies in Batfish), then convert it into {@link
 * RoutePolicy} (i.e., the model for routing policies in Expresso).
 *
 * <p>Since Batfish will encode BGP import (i.e., import routes from other protocols) and network
 * commands (i.e., announce a prefix if exists in the main RIB) into the export policies. We use
 * some ad hoc methods to restore these configurations from the deserialized {@link RoutingPolicy}s.
 */
public class RoutePolicyParser {
  public static SortedMap<String, RoutePolicy> parseRoutePolicies(
      Router router, JsonObject jsonRoutePolicies, JsonObject root) {
    // parse community exprs first
    deserializeCommunityExprs(router, root);

    SortedMap<String, RoutePolicy> routePolicies = new TreeMap<>();
    router.setRoutePolicies(routePolicies);
    for (Map.Entry<String, JsonElement> entry : jsonRoutePolicies.entrySet()) {
      List<RoutePolicy> rps = parseRoutePolicy(router, entry.getValue().getAsJsonObject());
      if (rps != null) {
        rps.forEach(rp -> routePolicies.put(rp.getName(), rp));
      }
    }
    return routePolicies;
  }

  public static List<RoutePolicy> parseRoutePolicy(Router router, JsonObject jsonRoutePolicy) {
    try {
      RoutingPolicy routingPolicy = deserializeRoutingPolicy(jsonRoutePolicy);
      return convertToRoutePolicy(router, routingPolicy);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Convert a {@link org.batfish.datamodel.routing_policy.RoutingPolicy} to {@link RoutePolicy}.
   */
  public static List<RoutePolicy> convertToRoutePolicy(Router router, RoutingPolicy routingPolicy) {
    ImmutableList.Builder<RoutePolicy> rps = new ImmutableList.Builder<>();

    String name = routingPolicy.getName();
    rps.add(convertToRoutePolicy(name, routingPolicy.getStatements(), router));
    if (name.contains("PEER_EXPORT")) {
      String peerAddr = name.substring(name.indexOf(":") + 1, name.length() - 1);
      rps.add(
          convertMatchStaticToRedistributionPolicy(
              peerAddr + "-REDISTRIBUTION", routingPolicy.getStatements(), router));
    }

    return rps.build();
  }

  static NestedRoutePolicy convertToRoutePolicy(
      String name, List<Statement> statements, Router router) {
    List<datamodel.routepolicy.statement.Statement> stmts =
        statements.stream()
            .map(statement -> statement.accept(new StmtVisitor(), router))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return new NestedRoutePolicy(name, stmts);
  }

  static NestedRoutePolicy convertMatchStaticToRedistributionPolicy(
      String name, List<Statement> statements, Router router) {
    List<Statement> matchStatics =
        statements.stream()
            .filter(statement -> statement instanceof If)
            .map(statement -> (If) statement)
            .filter(anIf -> isStaticGuard(anIf.getGuard()))
            .map(
                anIf ->
                    new If(
                        ((Conjunction) anIf.getGuard()).getConjuncts().get(1),
                        anIf.getTrueStatements(),
                        anIf.getFalseStatements()))
            .collect(Collectors.toList());
    return convertToRoutePolicy(name, matchStatics, router);
  }
}
