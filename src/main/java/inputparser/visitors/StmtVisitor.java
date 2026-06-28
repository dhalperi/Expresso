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
  public Statement visitExcludeAsPath(ExcludeAsPath excludeAsPath, Router router) {
    throw new UnsupportedOperationException("Haven't support ExcludeAsPath until now.");
  }

  @Override
  public Statement visitIf(If anIf, Router router) {
    // guard
    Match match = anIf.getGuard().accept(new BoolVisitor(), router);

    // true statements (some statements, e.g. comments, convert to null and are dropped, as in
    // RoutePolicyParser#convertToRoutePolicy)
    List<datamodel.routepolicy.statement.Statement> trueStmts =
        anIf.getTrueStatements().stream()
            .map(stmt -> stmt.accept(this, router))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

    // false statements
    List<datamodel.routepolicy.statement.Statement> falseStmts =
        anIf.getFalseStatements().stream()
            .map(stmt -> stmt.accept(this, router))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

    return new datamodel.routepolicy.statement.If(match, trueStmts, falseStmts);
  }

  @Override
  public Statement visitPrependAsPath(PrependAsPath prependAsPath, Router router) {
    return visitActionStatement(prependAsPath, router);
  }

  @Override
  public Statement visitReplaceAsesInAsSequence(ReplaceAsesInAsSequence replaceAsesInAsSequence) {
    throw new UnsupportedOperationException("Haven't support ReplaceAsesInAsSequence until now.");
  }

  @Override
  public Statement visitRemoveTunnelEncapsulationAttribute(
      RemoveTunnelEncapsulationAttribute removeTunnelEncapsulationAttribute, Router router) {
    throw new UnsupportedOperationException(
        "Haven't support RemoveTunnelEncapsulationAttribute until now.");
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
    // No-op: IS-IS metric/level attributes are not relevant to Expresso's BGP analysis.
    return null;
  }

  @Override
  public Statement visitSetIsisMetricType(SetIsisMetricType setIsisMetricType, Router router) {
    // No-op: IS-IS metric type is not relevant to Expresso's BGP analysis.
    return null;
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
    // No-op: OSPF metric type is not relevant to Expresso's BGP analysis.
    return null;
  }

  @Override
  public Statement visitSetTag(SetTag setTag, Router router) {
    return visitActionStatement(setTag, router);
  }

  @Override
  public Statement visitSetDefaultTag(SetDefaultTag setDefaultTag, Router router) {
    throw new UnsupportedOperationException("Haven't support SetDefaultTag until now.");
  }

  @Override
  public Statement visitSetOriginatorIp(SetOriginatorIp setOriginatorIp, Router router) {
    throw new UnsupportedOperationException("Haven't support SetOriginatorIp until now.");
  }

  @Override
  public Statement visitSetTunnelEncapsulationAttribute(
      SetTunnelEncapsulationAttribute setTunnelEncapsulationAttribute, Router router) {
    throw new UnsupportedOperationException(
        "Haven't support SetTunnelEncapsulationAttribute until now.");
  }

  @Override
  public Statement visitSetVarMetricType(SetVarMetricType setVarMetricType, Router router) {
    // No-op: metric type is not relevant to Expresso's BGP analysis.
    return null;
  }

  @Override
  public Statement visitSetWeight(SetWeight setWeight, Router router) {
    return visitActionStatement(setWeight, router);
  }

  /**
   * A {@link TraceableStatement} only adds tracing metadata around inner statements; it has no
   * routing semantics of its own, so we transparently convert its children. Multiple resulting
   * statements are wrapped in a single {@link datamodel.routepolicy.statement.If} that always
   * applies them (guard {@link datamodel.routepolicy.match.MatchTrue}).
   */
  @Override
  public Statement visitTraceableStatement(TraceableStatement traceableStatement, Router router) {
    List<datamodel.routepolicy.statement.Statement> inner =
        traceableStatement.getInnerStatements().stream()
            .map(stmt -> stmt.accept(this, router))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    if (inner.isEmpty()) {
      return null;
    }
    if (inner.size() == 1) {
      return inner.get(0);
    }
    return new datamodel.routepolicy.statement.If(
        datamodel.routepolicy.match.MatchTrue.INSTANCE, inner, java.util.Collections.emptyList());
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
      case ReturnLocalDefaultAction:
        return new ActionStatement(StaticAction.ReturnLocalDefaultAction);
      case ReturnTrue:
        return new ActionStatement(StaticAction.ReturnTrue);
      case ReturnFalse:
        return new ActionStatement(StaticAction.ReturnFalse);
      case SetDefaultActionAccept:
        return new ActionStatement(StaticAction.SetDefaultActionAccept);
      case SetDefaultActionReject:
        return new ActionStatement(StaticAction.SetDefaultActionReject);
      case SetLocalDefaultActionAccept:
        return new ActionStatement(StaticAction.SetLocalDefaultActionAccept);
      case SetLocalDefaultActionReject:
        return new ActionStatement(StaticAction.SetLocalDefaultActionReject);
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
