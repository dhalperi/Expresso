package datamodel.routepolicy.statement;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.routepolicy.NestedRoutePolicy;

import java.util.Objects;

public class CallStatement extends Statement {
  private final NestedRoutePolicy calledPolicy;

  public CallStatement(NestedRoutePolicy calledPolicy) {
    this.calledPolicy = calledPolicy;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    boolean oldCallStmtCtx = environment.isCallStatementContext();
    environment.setCallStatementContext(true);
    RouteFilterResult<R> result = calledPolicy.filter(route, environment);
    environment.setCallStatementContext(oldCallStmtCtx);
    result.setReturnFalse();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CallStatement that = (CallStatement) o;
    return Objects.equals(calledPolicy, that.calledPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calledPolicy);
  }
}
