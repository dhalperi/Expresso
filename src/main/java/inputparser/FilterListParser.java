package inputparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import datamodel.CanBeMatched;
import datamodel.Range;
import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.community.CommunityRegexConjunction;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.Mode;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import javafx.util.Pair;
import main.ExpressoLogger;

import java.util.*;
import java.util.function.Function;

public class FilterListParser {
  private static SortedMap<String, FilterList> parseLists(
      JsonObject jsonLists, Function<JsonObject, FilterList> func) {
    SortedMap<String, FilterList> map = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : jsonLists.entrySet()) {
      map.put(entry.getKey(), func.apply(entry.getValue().getAsJsonObject()));
    }
    return map;
  }

  public static SortedMap<String, FilterList> parsePrefixLists(JsonObject jsonPrefixLists) {
    return parseLists(jsonPrefixLists, FilterListParser::parsePrefixList);
  }

  public static SortedMap<String, FilterList> parseCommunityLists(JsonObject jsonCommunityLists) {
    return jsonCommunityLists == null
        ? new TreeMap<>()
        : parseLists(jsonCommunityLists, FilterListParser::parseCommunityList);
  }

  public static SortedMap<String, FilterList> parseAsPathLists(JsonObject jsonAsPathLists) {
    return parseLists(jsonAsPathLists, FilterListParser::parseAsPathList);
  }

  private static FilterList parseFilterList(
      JsonObject jsonList, Function<JsonObject, CanBeMatched> func) {
    String name = jsonList.get("name").getAsString();
    List<Pair<CanBeMatched, Mode>> lines = new ArrayList<>();

    JsonArray jsonArray = jsonList.getAsJsonArray("lines");
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject jsonLine = jsonArray.get(i).getAsJsonObject();
      Mode mode = Mode.valueOf(jsonLine.get("action").getAsString());
      CanBeMatched line = func.apply(jsonLine);
      lines.add(new Pair<>(line, mode));
    }

    return new FilterList(name, lines);
  }

  public static FilterList parsePrefixList(JsonObject jsonPrefixList) {
    return parseFilterList(
        jsonPrefixList,
        jsonObject -> {
          Prefix prefix = Prefix.of(jsonObject.get("ipWildcard").getAsString());
          String[] tmp = jsonObject.get("lengthRange").getAsString().split("-");
          Range<Integer> range = new Range<>(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
          return PrefixRange.of(prefix, range, prefix.getIp().asLong() != 0);
        });
  }

  public static FilterList parseCommunityList(JsonObject jsonCommunityList) {
    return parseFilterList(
        jsonCommunityList,
        jsonObject -> {
          JsonObject tmp0 = jsonObject.get("matchCondition").getAsJsonObject();
          String[] tmp1 = tmp0.get("class").getAsString().split("\\.");
          String cls = tmp1[tmp1.length - 1];
          switch (cls) {
            case "RegexCommunitySet":
              {
                String regex = tmp0.get("regex").getAsString();
                // todo check the rule of regex matching for huawei configs
                if (regex.startsWith("*")) {
                  regex = "." + regex;
                }
                return CommunityRegex.from(regex);
              }
            case "LiteralCommunity":
              {
                return CommunityRegex.from(
                    CommunityParser.parseLiteralCommunity(tmp0.get("community")));
              }
            case "LiteralCommunityConjunction":
              {
                JsonArray tmp = tmp0.get("requiredCommunities").getAsJsonArray();
                List<CommunityRegex> list = new ArrayList<>();
                for (int i = 0; i < tmp.size(); i++) {
                  list.add(CommunityRegex.from(CommunityParser.parseLiteralCommunity(tmp.get(i))));
                }
                return new CommunityRegexConjunction(list);
              }
            default:
              ExpressoLogger.log(
                  ExpressoLogger.LEVEL.ERROR,
                  "Unrecognized community filter match condition: " + cls);
              return null;
          }
        });
  }

  public static FilterList parseAsPathList(JsonObject jsonAsPathList) {
    return parseFilterList(
        jsonAsPathList,
        jsonObject -> {
          String matchStr = jsonObject.get("regex").getAsString();
          return new AsPathRegex(matchStr);
        });
  }
}
