package inputparser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Router;
import controlplane.route.BgpRoute;
import datamodel.community.Community;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.filterlist.Mode;
import datamodel.ipv4.Ip;
import datamodel.routepolicy.Node;
import datamodel.routepolicy.RoutePolicy;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetAsPath;
import datamodel.routepolicy.action.SetCommunity;
import datamodel.routepolicy.action.SetCost;
import datamodel.routepolicy.action.SetLocalPreference;
import datamodel.routepolicy.action.SetNextHop;
import datamodel.routepolicy.action.SetOrigin;
import datamodel.routepolicy.action.SetPreference;
import datamodel.routepolicy.action.SetWeight;
import datamodel.routepolicy.match.Match;
import datamodel.routepolicy.match.MatchAll;
import datamodel.routepolicy.match.MatchFilterList;
import datamodel.routepolicy.match.MatchLiteral;
import datamodel.routepolicy.match.MatchOne;
import datamodel.tag.Tag;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static util.JsonUtil.getAsBooleanDefaultFalse;
import static util.JsonUtil.getAsObjectDefaultNull;
import static util.JsonUtil.getAsStringDefaultNull;

public class RoutePolicyParser {
  public static SortedMap<String, RoutePolicy> parseRoutePolicies(
      Router router, JsonObject jsonRoutePolicies) {
    SortedMap<String, RoutePolicy> routePolicies = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : jsonRoutePolicies.entrySet()) {
      RoutePolicy routePolicy;
      if (entry.getValue().isJsonArray()) {
        // old json configuration format
        routePolicy = parseRoutePolicy(router, entry.getKey(), entry.getValue().getAsJsonArray());
      } else {
        // new json configuration format
        routePolicy =
            parseRoutePolicy(
                router, entry.getKey(), entry.getValue().getAsJsonObject().getAsJsonArray("nodes"));
      }
      routePolicies.put(entry.getKey(), routePolicy);
    }
    return routePolicies;
  }

  public static RoutePolicy parseRoutePolicy(
      Router router, String name, JsonArray jsonRoutePolicy) {
    ImmutableSortedMap.Builder<Integer, Node> nodes = ImmutableSortedMap.naturalOrder();
    for (int i = 0; i < jsonRoutePolicy.size(); i++) {
      JsonObject jsonRoutePolicyNode = jsonRoutePolicy.get(i).getAsJsonObject();

      int nodeId = Integer.parseInt(jsonRoutePolicyNode.get("node").getAsString());
      Mode mode = Mode.valueOf(jsonRoutePolicyNode.get("mode").getAsString());
      MatchAll matches =
          parseRoutePolicyMatches(router, jsonRoutePolicyNode.getAsJsonArray("matches"));
      List<Action> actions =
          parseRoutePolicyActions(router, jsonRoutePolicyNode.getAsJsonArray("actions"));
      Node routePolicyNode = new Node(mode, matches, actions);

      nodes.put(nodeId, routePolicyNode);
    }
    return new RoutePolicy(name, nodes.build());
  }

  public static MatchAll parseRoutePolicyMatches(Router router, JsonArray jsonMatches) {
    Multimap<String, Match> multimap = HashMultimap.create();
    for (int i = 0; i < jsonMatches.size(); i++) {
      JsonObject jsonMatch = jsonMatches.get(i).getAsJsonObject();
      Pair<String, Match> pair = parseRoutePolicyMatch(router, jsonMatch);
      multimap.put(pair.getKey(), pair.getValue());
    }
    List<MatchOne> matchOnes =
        multimap.asMap().values().stream()
            .map(matches -> new MatchOne(matches.toArray(new Match[0])))
            .collect(Collectors.toList());
    return new MatchAll(matchOnes);
  }

  public static Pair<String, Match> parseRoutePolicyMatch(Router router, JsonObject jsonMatch) {
    // todo make it more general
    String typeStr = jsonMatch.get("type").getAsString();
    String listName = jsonMatch.get("listName").getAsString();
    Match match;
    if (typeStr.equalsIgnoreCase("tag")) {
      Tag tag = new Tag(Integer.parseInt(listName));
      match = new MatchLiteral(tag);
    } else {
      FilterList list = router.getFilterList(getFilterType(typeStr), listName);
      match = new MatchFilterList(listName, list);
    }
    return new Pair<>(typeStr, match);
  }

  public static FilterType getFilterType(String typeStr) {
    if (typeStr.contains("prefix")) {
      return FilterType.PREFIX;
    } else if (typeStr.contains("community")) {
      return FilterType.COMMUNITY;
    } else {
      return FilterType.AS_PATH;
    }
  }

  public static List<Action> parseRoutePolicyActions(Router router, JsonArray jsonActions) {
    List<Action> actions = new ArrayList<>();
    for (int i = 0; i < jsonActions.size(); i++) {
      Action action = parseRoutePolicyAction(router, jsonActions.get(i).getAsJsonObject());
      if (action != null) {
        actions.add(action);
      }
    }
    return actions;
  }

  public static Action parseRoutePolicyAction(Router router, JsonObject jsonAction) {
    String type = jsonAction.get("type").getAsString();
    if (type.equalsIgnoreCase("local-preference")) {
      int localPreference = jsonAction.get("value").getAsInt();
      return SetLocalPreference.replace(localPreference);
    } else if (type.equalsIgnoreCase("cost")) {
      long cost = jsonAction.get("value").getAsLong();
      boolean additive = getAsBooleanDefaultFalse(jsonAction, "additive");
      return additive ? SetCost.increase(cost) : SetCost.replace(cost);
    } else if (type.equalsIgnoreCase("preferred-value")) {
      int preferredValue = jsonAction.get("value").getAsInt();
      return new SetWeight(preferredValue);
    } else if (type.equalsIgnoreCase("preference")) {
      int preference = jsonAction.get("value").getAsInt();
      return new SetPreference(preference);
    } else if (type.equalsIgnoreCase("community")) {
      boolean additive = jsonAction.get("additive").getAsBoolean();
      String tmp1 = jsonAction.get("value").getAsString();
      String[] tmp2 = tmp1.split(" ");
      List<Community> communities =
          Arrays.stream(tmp2).map(CommunityParser::parseCommunity).collect(Collectors.toList());
      return additive ? SetCommunity.increase(communities) : SetCommunity.replace(communities);
    } else if (type.equalsIgnoreCase("as-path")) {
      // options
      boolean additive = jsonAction.get("additive").getAsBoolean();
      boolean overwrite = jsonAction.get("overwrite").getAsBoolean();
      boolean none = jsonAction.get("none").getAsBoolean();
      boolean delete = jsonAction.get("delete").getAsBoolean();
      // as numbers
      String tmp1 = jsonAction.get("value").getAsString();
      List<Long> asns = new LinkedList<>();
      if (!Objects.equals(tmp1, "")) {
        String[] tmp2 = tmp1.split(" ");
        for (String tmp3 : tmp2) asns.add(BgpParser.parseAsNumber(tmp3));
      }
      return new SetAsPath(additive, overwrite, none, delete, asns);
    } else if (type.equalsIgnoreCase("ipv4-next-hop")) {
      Ip nextHopIp = Ip.of(getAsStringDefaultNull(jsonAction, "value"));
      boolean peerAddress = jsonAction.get("peerAddress").getAsBoolean();
      boolean blackhole = jsonAction.get("blackhole").getAsBoolean();
      return new SetNextHop(router, nextHopIp, peerAddress, false, blackhole);
    } else if (type.equalsIgnoreCase("origin")) {
      Long asNum = getAsObjectDefaultNull(jsonAction, "asNum", JsonElement::getAsLong);
      BgpRoute.OriginType origin =
          BgpRoute.OriginType.valueOf(jsonAction.get("origin").getAsString().toUpperCase());
      return new SetOrigin(asNum, origin);
    } else {
      return null;
    }
  }
}
