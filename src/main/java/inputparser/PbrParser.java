package inputparser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Router;
import datamodel.ipv4.Ip;
import datamodel.trafficpolicy.TrafficPolicy;
import datamodel.trafficpolicy.behavior.*;
import datamodel.trafficpolicy.classifier.*;
import javafx.util.Pair;
import util.JsonUtil;

import java.util.*;

public class PbrParser {
  public static SortedMap<String, TrafficClassifier> parseTrafficClassifiers(
      Router router, JsonObject jsonTrafficClassifier) {
    if (jsonTrafficClassifier == null) return ImmutableSortedMap.of();

    ImmutableSortedMap.Builder<String, TrafficClassifier> trafficClassifiers =
        ImmutableSortedMap.naturalOrder();

    jsonTrafficClassifier.entrySet().stream()
        .map(e -> parseTrafficClassifier(router, e.getValue().getAsJsonObject()))
        .forEach(classifier -> trafficClassifiers.put(classifier.getName(), classifier));

    return trafficClassifiers.build();
  }

  public static TrafficClassifier parseTrafficClassifier(
      Router router, JsonObject jsonTrafficClassifier) {
    String name = jsonTrafficClassifier.get("name").getAsString();
    boolean or = jsonTrafficClassifier.get("operator").getAsString().equals("or");

    List<Match> matches = new LinkedList<>();
    JsonArray jsonLines = jsonTrafficClassifier.getAsJsonArray("lines");
    for (int i = 0; i < jsonLines.size(); i++) {
      JsonObject jsonLine = jsonLines.get(i).getAsJsonObject();
      String type = jsonLine.get("type").getAsString();
      if (type.equals("any")) {
        matches.add(MatchAlways.TRUE);
      } else if (type.equals("acl")) {
        matches.add(new MatchAcl(router.getAcl(jsonLine.get("aclName").getAsString())));
      }
    }
    Match match =
        matches.size() == 0
            ? MatchAlways.FALSE
            : (matches.size() == 1
                ? matches.get(0)
                : (or ? new MatchAny(matches) : new MatchAll(matches)));

    return new TrafficClassifier(name, match);
  }

  public static SortedMap<String, TrafficBehavior> parseTrafficBehaviors(
      JsonObject jsonTrafficBehaviors) {
    if (jsonTrafficBehaviors == null)  return ImmutableSortedMap.of();

    ImmutableSortedMap.Builder<String, TrafficBehavior> trafficBehaviors =
        ImmutableSortedMap.naturalOrder();

    jsonTrafficBehaviors.entrySet().stream()
        .map(e -> parseTrafficBehavior(e.getValue().getAsJsonObject()))
        .forEach(behavior -> trafficBehaviors.put(behavior.getName(), behavior));

    return trafficBehaviors.build();
  }

  public static TrafficBehavior parseTrafficBehavior(JsonObject jsonTrafficBehavior) {
    String name = jsonTrafficBehavior.get("name").getAsString();

    List<Action> actions = new LinkedList<>();
    JsonArray jsonLines = jsonTrafficBehavior.getAsJsonArray("lines");
    for (int i = 0; i < jsonLines.size(); i++) {
      JsonObject jsonLine = jsonLines.get(i).getAsJsonObject();
      String type = jsonLine.get("type").getAsString();
      switch (type) {
        case "permit":
          actions.add(Permit.INSTANCE);
          break;
        case "deny":
          actions.add(Deny.INSTANCE);
          break;
        case "redirect":
          JsonArray jsonNexthops = jsonLine.getAsJsonArray("nexthops");
          List<Pair<Ip, String>> nexthops = new LinkedList<>();
          for (int j = 0; j < jsonNexthops.size(); j++) {
            JsonObject jsonNexthop = jsonNexthops.get(j).getAsJsonObject();
            Ip ip = Ip.parse(jsonNexthop.get("ip").getAsString());
            String intf = JsonUtil.getAsStringDefaultNull(jsonNexthop, "interface");
            nexthops.add(new Pair<>(ip, intf));
          }
          actions.add(new Redirect(nexthops));
          break;
      }
    }

    Action action =
        actions.size() == 0
            ? Permit.INSTANCE
            : (actions.size() == 1 ? actions.get(0) : new ActionChain(actions));
    return new TrafficBehavior(name, action);
  }

  public static SortedMap<String, TrafficPolicy> parseTrafficPolicies(
      Router router, JsonObject jsonTrafficPolicies) {
    if (jsonTrafficPolicies == null)  return ImmutableSortedMap.of();

    ImmutableSortedMap.Builder<String, TrafficPolicy> trafficPolicies =
        ImmutableSortedMap.naturalOrder();
    for (Map.Entry<String, JsonElement> e : jsonTrafficPolicies.entrySet()) {
      TrafficPolicy trafficPolicy = parseTrafficPolicy(router, e.getValue().getAsJsonObject());
      trafficPolicies.put(trafficPolicy.getName(), trafficPolicy);
    }
    return trafficPolicies.build();
  }

  public static TrafficPolicy parseTrafficPolicy(Router router, JsonObject jsonTrafficPolicy) {
    String name = jsonTrafficPolicy.get("name").getAsString();
    ImmutableList.Builder<Pair<TrafficClassifier, TrafficBehavior>> lines = ImmutableList.builder();

    // lines
    JsonArray jsonLines = jsonTrafficPolicy.getAsJsonArray("lines");
    for (int i = 0; i < jsonLines.size(); i++) {
      JsonObject jsonLine = jsonLines.get(i).getAsJsonObject();
      String classifier = jsonLine.get("classifier").getAsString();
      String behavior = jsonLine.get("behavior").getAsString();
      lines.add(
          new Pair<>(router.getTrafficClassifier(classifier), router.getTrafficBehavior(behavior)));
    }

    return new TrafficPolicy(name, lines.build());
  }
}
