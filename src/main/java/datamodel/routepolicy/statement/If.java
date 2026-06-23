package datamodel.routepolicy.statement;

import com.google.common.collect.ImmutableSet;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchCall;
import datamodel.routepolicy.match.MatchCallChain;
import datamodel.routepolicy.match.MatchCallExprContext;
import org.batfish.datamodel.routing_policy.Result;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class If extends Statement {
  private final Match guard;
  private final List<Statement> trueStatements;
  private final List<Statement> falseStatements;

  public If(Match guard, List<Statement> trueStatements, List<Statement> falseStatements) {
    this.guard = guard;
    this.trueStatements = trueStatements;
    this.falseStatements = falseStatements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    If anIf = (If) o;
    return Objects.equals(guard, anIf.guard)
        && Objects.equals(trueStatements, anIf.trueStatements)
        && Objects.equals(falseStatements, anIf.falseStatements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guard, trueStatements, falseStatements);
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    ImmutableSet.Builder<PrefixRange> set = ImmutableSet.builder();
    set.addAll(guard.collectPrefixRanges());
    trueStatements.forEach(t -> set.addAll(t.collectPrefixRanges()));
    falseStatements.forEach(f -> set.addAll(f.collectPrefixRanges()));
    return set.build();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    ImmutableSet.Builder<CommunityRegex> set = ImmutableSet.builder();
    set.addAll(guard.collectCommunityRegexes());
    trueStatements.forEach(t -> set.addAll(t.collectCommunityRegexes()));
    falseStatements.forEach(f -> set.addAll(f.collectCommunityRegexes()));
    return set.build();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    ImmutableSet.Builder<AsPathRegex> set = ImmutableSet.builder();
    set.addAll(guard.collectAsPathRegexes());
    trueStatements.forEach(t -> set.addAll(t.collectAsPathRegexes()));
    falseStatements.forEach(f -> set.addAll(f.collectAsPathRegexes()));
    return set.build();
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);

    RouteFilterResult<R> matchGuard = guard.filter(route, environment);
    RouteSet<R> matched = new RouteSet<>();
    RouteSet<R> unmatched = new RouteSet<>();
    // ad hoc solution
    if (guard instanceof MatchCall
        || guard instanceof MatchCallChain
        || guard instanceof MatchCallExprContext) {
      result.merge(matchGuard.getExit());
      matchGuard.getBooleanNonExit(true).values().forEach(matched::addAll);
      matchGuard.getBooleanNonExit(false).values().forEach(unmatched::addAll);
    } else {
      matched.addAll(matchGuard.getPermitted());
      unmatched.addAll(matchGuard.getDenied());
    }

    RouteFilterResult<R> trueResults = apply(route, environment, matched, trueStatements);
    RouteFilterResult<R> falseResults = apply(route, environment, unmatched, falseStatements);
    // add true branch results
    result.merge(trueResults);
    // add false branch results
    result.merge(falseResults);

    return result;
  }

  private <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> apply(
          R originalRoute,
          RouteFilterEnvironment<R> environment,
          RouteSet<R> routes,
          List<Statement> statements) {
    RouteFilterResult<R> result = new RouteFilterResult<>(originalRoute);

    RouteSet<R> fallThrough = routes;
    for (Statement stmt : statements) {
      RouteSet<R> fallThroughTmp = new RouteSet<>();
      for (R route : fallThrough.getAllRoutes()) {
        RouteFilterResult<R> resultTmp = stmt.filter(route, environment);

        // handle explicitly accepted or rejected routes
        result.merge(resultTmp.getExit());

        // handle returned (either true or false) routes
        result.merge(resultTmp.getReturn());

        // handle fall through routes
        resultTmp.getNonExitReturn().values().forEach(fallThroughTmp::addAll);
      }
      fallThrough = fallThroughTmp;
    }

    List<R> list = fallThrough.getAllRoutes();
    result
        .getResults(Result.builder().setFallThrough(true).build())
        .values()
        .forEach(rs -> rs.addAll(list));
    return result;
  }
}
