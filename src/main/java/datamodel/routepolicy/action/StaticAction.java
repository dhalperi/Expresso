package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.route.RouteFilterEnvironment;
import org.batfish.datamodel.routing_policy.Result;

/** Used for {@link datamodel.routepolicy.NestedRoutePolicy}. */
public enum StaticAction implements Action {
  ExitAccept,
  ExitReject,
  ReturnTrue,
  ReturnFalse,
  Return,
  FallThrough,
  SetDefaultActionAccept,
  SetDefaultActionReject;

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    switch (this) {
      case ExitAccept:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setExit(true).setBooleanValue(true).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case ExitReject:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setExit(true).setBooleanValue(false).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case ReturnTrue:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setReturn(true).setBooleanValue(true).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case ReturnFalse:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setReturn(true).setBooleanValue(false).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case Return:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setReturn(true).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case FallThrough:
        if (environment.getResult() != null)
          environment
              .getResult()
              .getResults(Result.builder().setReturn(true).setFallThrough(true).build())
              .values()
              .forEach(rs -> rs.add(route));
        return;
      case SetDefaultActionAccept:
        environment.setDefaultAction(true);
        break;
      case SetDefaultActionReject:
        environment.setDefaultAction(false);
        break;
    }
    if (environment.getResult() != null)
      environment.getResult().getResults(new Result(false)).values().forEach(rs -> rs.add(route));
  }
}
