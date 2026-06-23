package inputparser.visitors;

import controlplane.network.Router;
import datamodel.routepolicy.NestedRoutePolicy;
import datamodel.routepolicy.RoutePolicy;
import datamodel.routepolicy.action.RemovePrivateAs;
import datamodel.routepolicy.action.StaticAction;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.statement.ActionStatement;
import datamodel.routepolicy.statement.Statement;
import inputparser.bfvi.RoutePolicyParser;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.statement.*;

import java.util.List;
import java.util.stream.Collectors;

/** Used by {@link RoutePolicyParser} */
public class StmtVisitor implements StatementVisitor<Statement, Router> {
  @Override
  public Statement visitAddCommunity(AddCommunity addCommunity, Router router) {
    return visitActionStatement(addCommunity, router);
  }

  @Override
  public Statement visitBufferedStatement(BufferedStatement bufferedStatement, Router router) {
    throw new UnsupportedOperationException("Haven't support BufferedStatement until now.");
  }

  @Override
  public Statement visitCallStatement(CallStatement callStatement, Router router) {
    RoutePolicy calledPolicy = router.getRoutePolicy(callStatement.getCalledPolicyName());
    if (!(calledPolicy instanceof NestedRoutePolicy)) {
      throw new IllegalArgumentException();
    }
    return new datamodel.routepolicy.statement.CallStatement((NestedRoutePolicy) calledPolicy);
  }

  @Override
  public Statement visitComment(Comment comment, Router router) {
    // do nothing
    return null;
  }

  @Override
  public Statement visitDeleteCommunity(DeleteCommunity deleteCommunity, Router router) {
    return visitActionStatement(deleteCommunity, router);
  }

  @Override
  public Statement visitIf(If anIf, Router router) {
    // guard
    Match match = anIf.getGuard().accept(new BoolVisitor(), router);

    // true statements
    List<datamodel.routepolicy.statement.Statement> trueStmts =
        anIf.getTrueStatements().stream()
            .map(stmt -> stmt.accept(this, router))
            .collect(Collectors.toList());

    // false statements
    List<datamodel.routepolicy.statement.Statement> falseStmts =
        anIf.getFalseStatements().stream()
            .map(stmt -> stmt.accept(this, router))
            .collect(Collectors.toList());

    return new datamodel.routepolicy.statement.If(match, trueStmts, falseStmts);
  }

  @Override
  public Statement visitOverwriteAsPath(OverwriteAsPath overwriteAsPath, Router router) {
    return visitActionStatement(overwriteAsPath, router);
  }

  @Override
  public Statement visitPrependAsPath(PrependAsPath prependAsPath, Router router) {
    return visitActionStatement(prependAsPath, router);
  }

  @Override
  public Statement visitSetAdministrativeCost(
      SetAdministrativeCost setAdministrativeCost, Router router) {
    return visitActionStatement(setAdministrativeCost, router);
  }

  @Override
  public Statement visitSetCommunities(SetCommunities setCommunities, Router router) {
    return visitActionStatement(setCommunities, router);
  }

  @Override
  public Statement visitSetCommunity(SetCommunity setCommunity, Router router) {
    return visitActionStatement(setCommunity, router);
  }

  @Override
  public Statement visitSetDefaultPolicy(SetDefaultPolicy setDefaultPolicy, Router router) {
    return new datamodel.routepolicy.statement.SetDefaultPolicy(
        setDefaultPolicy.getDefaultPolicy());
  }

  @Override
  public Statement visitSetEigrpMetric(SetEigrpMetric setEigrpMetric, Router router) {
    throw new UnsupportedOperationException("Haven't support SetEigrpMetric until now.");
  }

  @Override
  public Statement visitSetIsisLevel(SetIsisLevel setIsisLevel, Router router) {
    throw new UnsupportedOperationException("Haven't support SetIsisLevel until now.");
  }

  @Override
  public Statement visitSetIsisMetricType(SetIsisMetricType setIsisMetricType, Router router) {
    throw new UnsupportedOperationException("Haven't support SetIsisMetricType until now.");
  }

  @Override
  public Statement visitSetLocalPreference(SetLocalPreference setLocalPreference, Router router) {
    return visitActionStatement(setLocalPreference, router);
  }

  @Override
  public Statement visitSetMetric(SetMetric setMetric, Router router) {
    return visitActionStatement(setMetric, router);
  }

  @Override
  public Statement visitSetNextHop(SetNextHop setNextHop, Router router) {
    return visitActionStatement(setNextHop, router);
  }

  @Override
  public Statement visitSetOrigin(SetOrigin setOrigin, Router router) {
    return visitActionStatement(setOrigin, router);
  }

  @Override
  public Statement visitSetOspfMetricType(SetOspfMetricType setOspfMetricType, Router router) {
    throw new UnsupportedOperationException("Haven't support SetOspfMetricType until now.");
  }

  @Override
  public Statement visitSetTag(SetTag setTag, Router router) {
    return visitActionStatement(setTag, router);
  }

  @Override
  public Statement visitSetVarMetricType(SetVarMetricType setVarMetricType, Router router) {
    throw new UnsupportedOperationException("Haven't support SetVarMetricType until now.");
  }

  @Override
  public Statement visitSetWeight(SetWeight setWeight, Router router) {
    return visitActionStatement(setWeight, router);
  }

  @Override
  public Statement visitStaticStatement(Statements.StaticStatement staticStatement, Router router) {
    switch (staticStatement.getType()) {
      case ExitAccept:
        return new ActionStatement(StaticAction.ExitAccept);
      case ExitReject:
        return new ActionStatement(StaticAction.ExitReject);
      case FallThrough:
        return new ActionStatement(StaticAction.FallThrough);
      case RemovePrivateAs:
        return new ActionStatement(RemovePrivateAs.INSTANCE);
      case Return:
        return new ActionStatement(StaticAction.Return);
      case ReturnTrue:
        return new ActionStatement(StaticAction.ReturnTrue);
      case ReturnFalse:
        return new ActionStatement(StaticAction.ReturnFalse);
      case SetDefaultActionAccept:
        return new ActionStatement(StaticAction.SetDefaultActionAccept);
      case SetDefaultActionReject:
        return new ActionStatement(StaticAction.SetDefaultActionReject);
      default:
        throw new UnsupportedOperationException(
            String.format(
                "Haven't support static statement %s until now.", staticStatement.getType()));
    }
  }

  private ActionStatement visitActionStatement(
      org.batfish.datamodel.routing_policy.statement.Statement statement, Router router) {
    return new ActionStatement(statement.accept(new StatementToAction(), router));
  }
}
