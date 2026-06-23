package datamodel.route;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;

import java.util.Collection;

public interface RouteFilter {
  /**
   * The last two parameters are only used by {@link RouteFilter#filter(DynamicRoute,
   * RouteFilterEnvironment)}. Since a {@link datamodel.routepolicy.Node} will apply actions to
   * {@link controlplane.route.Route}s.
   */
  <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>> RouteFilterResult<R> filter(
      R route, RouteFilterEnvironment<R> environment);

  static <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>, F extends RouteFilter>
      RouteFilterResult<R> matchAll(
          R route, Collection<F> filters, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    if (filters.isEmpty()) {
      // if no match conditions, permit all
      result.getPermitted().add(route.toBuilder().build());
    } else {
      RouteSet<R> remains = new RouteSet<>(route);
      for (F filter : filters) {
        RouteSet<R> remainsTmp = new RouteSet<>();
        for (R remain : remains.getAllRoutes()) {
          RouteFilterResult<R> resultTmp = filter.filter(remain, environment);
          // denied by one filter -> denied
          result.getDenied().addAll(resultTmp.getDenied());
          remainsTmp.addAll(resultTmp.getPermitted());
        }
        remains = remainsTmp;
      }
      result.getPermitted().addAll(remains);
    }
    return result;
  }

  static <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>, F extends RouteFilter>
      RouteFilterResult<R> matchOne(
          R route, Collection<F> filters, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    if (filters.isEmpty()) {
      // if no match conditions, permit all
      result.getPermitted().add(route.toBuilder().build());
    } else {
      RouteSet<R> remains = new RouteSet<>(route);
      for (F filter : filters) {
        RouteSet<R> remainsTmp = new RouteSet<>();
        for (R remain : remains.getAllRoutes()) {
          RouteFilterResult<R> resultTmp = filter.filter(remain, environment);
          // permitted by one filter -> permitted
          result.getPermitted().addAll(resultTmp.getPermitted());
          remainsTmp.addAll(resultTmp.getDenied());
        }
        remains = remainsTmp;
      }
      result.getDenied().addAll(remains);
    }
    return result;
  }
}
