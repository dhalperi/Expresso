package inputparser.visitors;

import controlplane.network.Router;
import datamodel.aspath.AsPathRegex;
import datamodel.filterlist.FilterType;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchFilterList;
import datamodel.routepolicy.match.MatchLiteral;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchAny;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchExprReference;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchExprVisitor;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchRegex;
import org.batfish.datamodel.routing_policy.as_path.AsSetsMatchingRanges;
import org.batfish.datamodel.routing_policy.as_path.HasAsPathLength;
import org.batfish.minesweeper.SymbolicAsPathRegex;

import java.util.stream.Collectors;

import static inputparser.visitors.BooleanVisitor.matchOne;

/**
 * Converts an {@link org.batfish.datamodel.routing_policy.as_path.AsPathMatchExpr} into Expresso's
 * {@link Match} model. This is the modern replacement for the legacy {@code expr.MatchAsPath} /
 * {@code AsPathSetExpr} handling: HEAD Batfish models AS-path matching as a {@link
 * org.batfish.datamodel.routing_policy.as_path.MatchAsPath} of an {@code AsPathExpr} (the path under
 * test, always {@code InputAsPath} for Expresso's purposes) and an {@code AsPathMatchExpr} (the
 * predicate), which this visitor handles.
 */
public class AsPathMatchExprToMatch implements AsPathMatchExprVisitor<Match, Router> {
  @Override
  public Match visitAsPathMatchAny(AsPathMatchAny asPathMatchAny, Router router) {
    return matchOne(
        asPathMatchAny.getDisjuncts().stream()
            .map(expr -> expr.accept(this, router))
            .collect(Collectors.toList()));
  }

  @Override
  public Match visitAsPathMatchExprReference(
      AsPathMatchExprReference asPathMatchExprReference, Router router) {
    String listName = asPathMatchExprReference.getName();
    return new MatchFilterList(listName, router.getFilterList(FilterType.AS_PATH, listName));
  }

  @Override
  public Match visitAsPathMatchRegex(AsPathMatchRegex asPathMatchRegex, Router router) {
    return new MatchLiteral(new AsPathRegex(asPathMatchRegex.getRegex()));
  }

  @Override
  public Match visitAsSetsMatchingRanges(AsSetsMatchingRanges asSetsMatchingRanges, Router router) {
    // AsSetsMatchingRanges is Batfish's structured form of common AS-path regexes (e.g. a Juniper
    // `as-path` like "^293 .*"): a sequence of AS-number ranges with optional start/end anchors.
    // Reuse Batfish's own canonical translation to an automaton-library regex string, then wrap it
    // in Expresso's AsPathRegex, so the matching semantics stay identical to Batfish.
    return new MatchLiteral(
        new AsPathRegex(new SymbolicAsPathRegex(asSetsMatchingRanges).getRegex()));
  }

  @Override
  public Match visitHasAsPathLength(HasAsPathLength hasAsPathLength, Router router) {
    // AS-path length matching is not modeled by Expresso.
    throw new UnsupportedOperationException();
  }
}
