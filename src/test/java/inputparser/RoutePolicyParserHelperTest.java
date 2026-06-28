package inputparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import bdd.BddManager;
import inputparser.ConfigurationParser;
import main.Controller;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class RoutePolicyParserHelperTest {
  @Test
  public void testParseRoutePolicy() {
    // Parsing routes through the BDD layer, so a manager must exist.
    Controller.bddManager = new BddManager();
    // Use the example network: its VI is compatible with the current Batfish model. (The committed
    // internet2 VI was produced by an older Batfish and contains classes since removed, e.g.
    // MatchIpv6, so it no longer deserializes; regenerate it from raw configs to use it here.)
    Path config = Paths.get("networks/example/configs/pr1.json");
    ConfigurationParser parser = new ConfigurationParser();
    parser.parseFile(config);
  }

  @Test
  public void parse() {
    Path cfgs = Paths.get("networks/internet2/configs");
    File cfgDir = cfgs.toFile();
    if (cfgDir.isDirectory()) {
      String[] cfgFiles = cfgDir.list();
      assert cfgFiles != null;

      SortedMap<String, SortedSet<String>> usedPolicies = new TreeMap<>();
      SortedSet<String> groups = new TreeSet<>();
      for (String cfgFile : cfgFiles) {
        JsonParser parser = new JsonParser();
        try {
          JsonObject root =
              (JsonObject) parser.parse(new FileReader(cfgs.resolve(cfgFile).toFile()));
          JsonObject jsonBgpNeighbors =
              root.getAsJsonObject("vrfs")
                  .getAsJsonObject("default")
                  .getAsJsonObject("bgpProcess")
                  .getAsJsonObject("neighbors");
          jsonBgpNeighbors
              .entrySet()
              .forEach(
                  e -> {
                    JsonArray imports =
                        e.getValue()
                            .getAsJsonObject()
                            .getAsJsonObject("ipv4UnicastAddressFamily")
                            .getAsJsonArray("importPolicySources");
                    imports.forEach(
                        im ->
                            usedPolicies
                                .computeIfAbsent(im.getAsString(), ii -> new TreeSet<>())
                                .add(cfgFile.substring(0, cfgFile.indexOf("."))));
                    JsonArray exports =
                        e.getValue()
                            .getAsJsonObject()
                            .getAsJsonObject("ipv4UnicastAddressFamily")
                            .getAsJsonArray("exportPolicySources");
                    exports.forEach(
                        ex ->
                            usedPolicies
                                .computeIfAbsent(ex.getAsString(), ee -> new TreeSet<>())
                                .add(cfgFile.substring(0, cfgFile.indexOf("."))));
                    JsonElement jsonGroup = e.getValue().getAsJsonObject().get("group");
                    if (!jsonGroup.isJsonNull()) groups.add(jsonGroup.getAsString());
                  });
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
      System.out.println(usedPolicies.size());
      System.out.println(
          usedPolicies.entrySet().stream()
              .map(
                  e ->
                      e.getKey()
                          + " ("
                          + e.getValue().size()
                          + "): "
                          + String.join(", ", e.getValue()))
              .collect(Collectors.joining("\n")));

      System.out.println("--------------------------------");

      System.out.println(groups.size());
      System.out.println(String.join("\n", groups));
    }
  }
}
