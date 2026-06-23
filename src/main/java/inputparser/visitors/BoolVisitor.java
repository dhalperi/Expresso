package inputparser.visitors;

import controlplane.network.Router;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchCall;
import datamodel.routepolicy.match.MatchCallChain;
import datamodel.routepolicy.match.MatchCallExprContext;
import datamodel.routepolicy.match.MatchFalse;
import datamodel.routepolicy.match.MatchTrue;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.FirstMatchChain;

import java.util.List;
import java.util.stream.Collectors;

public class BoolVisitor extends BooleanVisitor {
  @Override
  public Match visitBooleanExprs(BooleanExprs.StaticBooleanExpr staticBooleanExpr, Router router) {
    switch (staticBooleanExpr.getType()) {
      case CallExprContext:
        return MatchCallExprContext.INSTANCE;
      case True:
        return MatchTrue.INSTANCE;
      case False:
        return MatchFalse.INSTANCE;
      default:
        throw new UnsupportedOperationException(staticBooleanExpr.getType().toString());
    }
  }

  @Override
  public Match visitFirstMatchChain(FirstMatchChain firstMatchChain, Router router) {
    List<Match> matches =
        firstMatchChain.getSubroutines().stream()
            .map(subroutine -> subroutine.accept(this, router))
            .collect(Collectors.toList());
    if (matches.stream().allMatch(match -> match instanceof MatchCall)) {
      return new MatchCallChain(
          matches.stream().map(match -> (MatchCall) match).collect(Collectors.toList()));
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
