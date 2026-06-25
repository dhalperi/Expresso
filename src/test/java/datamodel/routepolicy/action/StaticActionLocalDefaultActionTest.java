package datamodel.routepolicy.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import datamodel.route.RouteSet;
import java.util.Collection;
import main.Controller;
import org.batfish.datamodel.routing_policy.Result;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the modeling of Batfish's per-policy "local default action" (used for Cisco route-map
 * fall-off): {@link StaticAction#SetLocalDefaultActionAccept} / {@code ...Reject} mutate the
 * environment, and {@link StaticAction#ReturnLocalDefaultAction} returns with that value.
 */
public class StaticActionLocalDefaultActionTest {

  @Before
  public void setUp() {
    // RouteSet#add ORs prefix BDDs, so a BDD manager must exist.
    Controller.bddManager = new BddManager();
  }

  private static RouteFilterEnvironment<BgpRoute> envWithResult(BgpRoute route) {
    RouteFilterEnvironment<BgpRoute> env =
        new RouteFilterEnvironment.Builder<BgpRoute>().setIn(true).setLocalConfig(null).build();
    env.setResult(new RouteFilterResult<>(route));
    return env;
  }

  /** Total number of routes across the RouteSets matching the given {@link Result} encoding. */
  private static int routeCount(RouteFilterEnvironment<BgpRoute> env, Result result) {
    Collection<RouteSet<BgpRoute>> sets = env.getResult().getResults(result).values();
    return sets.stream().mapToInt(rs -> rs.getAllRoutes().size()).sum();
  }

  @Test
  public void localDefaultActionDefaultsToFalse() {
    assertFalse(
        new RouteFilterEnvironment.Builder<BgpRoute>()
            .setIn(true)
            .setLocalConfig(null)
            .build()
            .isLocalDefaultAction());
  }

  @Test
  public void setLocalDefaultActionAcceptThenReject() {
    BgpRoute route = new BgpRouteBuilder().setPrefixesBdd(1).build();
    RouteFilterEnvironment<BgpRoute> env = envWithResult(route);

    StaticAction.SetLocalDefaultActionAccept.act(route, env);
    assertTrue(env.isLocalDefaultAction());

    StaticAction.SetLocalDefaultActionReject.act(route, env);
    assertFalse(env.isLocalDefaultAction());
  }

  @Test
  public void returnLocalDefaultActionReturnsTrueWhenAccept() {
    BgpRoute route = new BgpRouteBuilder().setPrefixesBdd(1).build();
    RouteFilterEnvironment<BgpRoute> env = envWithResult(route);
    env.setLocalDefaultAction(true);

    StaticAction.ReturnLocalDefaultAction.act(route, env);

    // The route should land in the {return=true, booleanValue=true} bucket and nowhere with
    // booleanValue=false.
    assertEquals(
        1, routeCount(env, Result.builder().setReturn(true).setBooleanValue(true).build()));
    assertEquals(
        0, routeCount(env, Result.builder().setReturn(true).setBooleanValue(false).build()));
  }

  @Test
  public void returnLocalDefaultActionReturnsFalseByDefault() {
    BgpRoute route = new BgpRouteBuilder().setPrefixesBdd(1).build();
    RouteFilterEnvironment<BgpRoute> env = envWithResult(route);
    // localDefaultAction defaults to false (Cisco route-map fall-off = deny).

    StaticAction.ReturnLocalDefaultAction.act(route, env);

    assertEquals(
        1, routeCount(env, Result.builder().setReturn(true).setBooleanValue(false).build()));
    assertEquals(
        0, routeCount(env, Result.builder().setReturn(true).setBooleanValue(true).build()));
  }
}
