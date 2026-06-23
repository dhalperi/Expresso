package datamodel.community;

import bdd.BddManager;
import controlplane.route.BgpRoute;
import controlplane.route.builder.BgpRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.Mode;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import javafx.util.Pair;
import main.Controller;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SymbolicCommunityMatchingTest {
  private static final Pair<CanBeMatched, Mode> permitLiteralCommunity =
      new Pair<>(CommunityRegex.from(StandardCommunity.of(65534, 9808)), Mode.PERMIT);
  private static final Pair<CanBeMatched, Mode> permitCommunityRegex =
      new Pair<>(CommunityRegex.from("^6520.:1301."), Mode.PERMIT);
  private static final Pair<CanBeMatched, Mode> permitCommunityRegexConjunction =
      new Pair<>(
          new CommunityRegexConjunction(
              CommunityRegex.from(StandardCommunity.of(65534, 4134)),
              CommunityRegex.from(StandardCommunity.of(65415, 12011))),
          Mode.PERMIT);

  @Test
  public void testSymbolicCommunityMatching() {
    Controller.bddManager = new BddManager();

    FilterList cl1 =
        new FilterList("list1", Stream.of(permitLiteralCommunity).collect(Collectors.toList()));
    FilterList cl2 =
        new FilterList("list2", Stream.of(permitCommunityRegex).collect(Collectors.toList()));
    FilterList cl3 =
        new FilterList(
            "list3", Stream.of(permitCommunityRegexConjunction).collect(Collectors.toList()));

    RouteFilterResult<BgpRoute> result1 =
        cl1.filter(
            new BgpRouteBuilder().setPrefixesBdd(1).build(),
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    print(result1);

    RouteFilterResult<BgpRoute> result2 =
        cl2.filter(
            new BgpRouteBuilder().setPrefixesBdd(1).build(),
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    print(result2);

    RouteFilterResult<BgpRoute> result3 =
        cl3.filter(
            new BgpRouteBuilder().setPrefixesBdd(1).build(),
            new RouteFilterEnvironment.Builder<BgpRoute>()
                .setIn(true)
                .setLocalConfig(null)
                .build());
    print(result3);
  }

  public static void print(RouteFilterResult<BgpRoute> result) {
    if (result.getPermitted().isEmpty()) {
      System.out.println("No route is permitted");
    } else {
      System.out.println(
          result.getPermitted().getAllRoutes().stream()
              .map(BgpRoute::toString)
              .collect(Collectors.joining("\n")));
    }
  }
}
