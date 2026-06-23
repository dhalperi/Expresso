package datamodel.routepolicy;

import com.google.common.collect.ImmutableList;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.statement.Statement;
import org.batfish.datamodel.routing_policy.Result;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a temporary class to encode {@link org.batfish.datamodel.routing_policy.RoutingPolicy},
 * where one {@link org.batfish.datamodel.routing_policy.RoutingPolicy} can call other {@link
 * org.batfish.datamodel.routing_policy.RoutingPolicy}s.
 *
 * <p>{@link org.batfish.datamodel.routing_policy.statement.Statements#FallThrough} is an important
 * feature to encode.
 */
public class NestedRoutePolicy extends RoutePolicy {
  private final List<Statement> statements;

  public NestedRoutePolicy(String name, List<Statement> statements) {
    super(name);
    this.statements =
        statements instanceof ImmutableList ? statements : ImmutableList.copyOf(statements);
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);

    RouteSet<R> fallThrough = new RouteSet<>(route);
    for (Statement stmt : statements) {
      RouteSet<R> fallThroughTmp = new RouteSet<>();
      for (R r : fallThrough.getAllRoutes()) {
        RouteFilterResult<R> resultTmp = stmt.filter(r, environment);

        // update accepted and rejected routes, they won't go through the following statements and
        // policies. E.g., encountering "then accept" or "then reject" in juniper policy statements.
        result.merge(resultTmp.getExit());

        // update returned routes, they won't go through the following statements.
        // E.g., encountering "next policy" in juniper policy statements.
        result.merge(RouteFilterResult.setReturnFalse(resultTmp.getReturn()));

        // fall through routes have to go through the following statements.
        resultTmp.getNonExitReturn().values().forEach(fallThroughTmp::addAll);
      }
      fallThrough = fallThroughTmp;
    }

    List<R> list = fallThrough.getAllRoutes();
    result
        .getResults(
            Result.builder()
                .setFallThrough(true)
                .setBooleanValue(environment.isDefaultAction())
                .build())
        .values()
        .forEach(rs -> rs.addAll(list));

    if (!environment.isCallExprContext()) {
      // this is the out-most route policy, add return true and fall through true to permitted, add
      // return false and fall through false to denied.
      result.finalization();
    }

    return result;
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return statements.stream()
        .flatMap(stmt -> stmt.collectPrefixRanges().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return statements.stream()
        .flatMap(stmt -> stmt.collectCommunityRegexes().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return statements.stream()
        .flatMap(stmt -> stmt.collectAsPathRegexes().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    NestedRoutePolicy that = (NestedRoutePolicy) o;
    return Objects.equals(getName(), that.getName()) && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), statements);
  }
}
