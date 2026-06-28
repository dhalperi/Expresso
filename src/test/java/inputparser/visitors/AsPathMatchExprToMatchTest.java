package inputparser.visitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import controlplane.network.Router;
import datamodel.aspath.AsPathRegex;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchFilterList;
import datamodel.routepolicy.match.MatchLiteral;
import datamodel.routepolicy.match.MatchOne;
import java.util.Set;
import org.batfish.datamodel.routing_policy.as_path.AsPathExprReference;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchAny;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchExprReference;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchRegex;
import org.batfish.datamodel.routing_policy.as_path.InputAsPath;
import org.batfish.datamodel.routing_policy.as_path.MatchAsPath;
import org.junit.Test;

/**
 * Tests that the modern Batfish {@code as_path} model is translated into Expresso's {@link Match}
 * model by {@link AsPathMatchExprToMatch} (invoked from {@link BooleanVisitor#visitMatchAsPath}).
 */
public class AsPathMatchExprToMatchTest {
  private static final BooleanVisitor BOOLEAN_VISITOR = new BooleanVisitor();

  /** A bare router with one named AS-path filter list so reference resolution can be tested. */
  private static Router routerWithAsPathList(String name, FilterList list) {
    Router router = new Router("r1");
    router.getFilterLists().put(FilterType.AS_PATH, name, list);
    return router;
  }

  @Test
  public void testAsPathMatchRegex() {
    MatchAsPath expr =
        MatchAsPath.of(InputAsPath.instance(), AsPathMatchRegex.of("(^| )65001($| )"));
    Match match = expr.accept(BOOLEAN_VISITOR, new Router("r1"));

    assertTrue(match instanceof MatchLiteral);
    Set<AsPathRegex> regexes = match.collectAsPathRegexes();
    assertEquals(1, regexes.size());
    assertEquals(new AsPathRegex("(^| )65001($| )"), regexes.iterator().next());
  }

  @Test
  public void testAsPathMatchExprReferenceResolvesToFilterList() {
    FilterList list = new FilterList("as-path-1", ImmutableList.of());
    Router router = routerWithAsPathList("as-path-1", list);

    MatchAsPath expr =
        MatchAsPath.of(InputAsPath.instance(), AsPathMatchExprReference.of("as-path-1"));
    Match match = expr.accept(BOOLEAN_VISITOR, router);

    assertTrue(match instanceof MatchFilterList);
    assertEquals("as-path-1", ((MatchFilterList) match).getListName());
  }

  @Test
  public void testAsPathMatchAnyIsDisjunction() {
    MatchAsPath expr =
        MatchAsPath.of(
            InputAsPath.instance(),
            AsPathMatchAny.of(
                ImmutableList.of(
                    AsPathMatchRegex.of("(^| )1($| )"), AsPathMatchRegex.of("(^| )2($| )"))));
    Match match = expr.accept(BOOLEAN_VISITOR, new Router("r1"));

    // Two distinct regex alternatives become a MatchOne (disjunction) collecting both regexes.
    assertTrue(match instanceof MatchOne);
    assertEquals(2, match.collectAsPathRegexes().size());
  }

  /** The AsPathExpr under test is ignored (it is always the route's own input path). */
  @Test
  public void testAsPathExprIsIgnored() {
    MatchAsPath expr =
        MatchAsPath.of(AsPathExprReference.of("anything"), AsPathMatchRegex.of("(^| )7($| )"));
    Match match = expr.accept(BOOLEAN_VISITOR, new Router("r1"));
    assertTrue(match instanceof MatchLiteral);
  }
}
