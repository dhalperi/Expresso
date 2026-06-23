package propertycheckers.routing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import controlplane.process.bgp.ISP;
import controlplane.route.BgpRoute;
import datamodel.ipv4.PrefixRange;
import datamodel.path.PathHop;
import main.Controller;
import main.ExpressoLogger;
import util.TimeUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static propertycheckers.JsonPrinter.printPrefixRange;
import static propertycheckers.JsonPrinter.printPrefixRangeWithEnvc;
import static application.info.AsInfo.*;
import static util.MemorizeUtil.*;

/**
 * This class checks for Gao-Rexford violations, i.e., whether our network leaks route from one
 * non-customer external neighbor to another non-customer external neighbor. By default, Expresso
 * infers the role (i.e., provider, peer, customer) of an external neighbor by checking its ASN (see
 * {@link application.info.AsInfo}, which currently only considers several well-known ASNs owned by
 * ISPs in China).
 *
 * <p>Users can directly change the {@link application.info.AsInfo} to specify
 * providers/peers/customers of their network. Accepting user-provided YAML files specifying role
 * infos of external neighbors will be supported in the future.
 */
public class GaoRexfordChecker extends RoutingChecker {

  // initialize it to avoid null pointer exception
  static HashMap<ISP, List<BgpRoute>> ALL_RESULTS = new HashMap<>();

  public static HashMap<ISP, List<BgpRoute>> check(boolean writeToJson) {
    loadInternalPrefixes();
    ExpressoLogger.pushContext("GAO REXFORD");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    ALL_RESULTS = new HashMap<>();

    Collection<ISP> isps = Controller.cpExecutor.bgpTopology.getISPs();
    Collection<ISP> nonCustomers =
        isps.stream().filter(isp -> !isCustomer(isp.getAs())).collect(Collectors.toSet());

    nonCustomers.forEach(nonCustomer -> check(nonCustomer, nonCustomers));

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();

    if (writeToJson) {
      OUTPUT =
          Controller.storage.output._outputBase.resolve(
              WITH_ENVC ? "gao_rexford" : "gao_rexford_without_envc");
      createDirIfAbsentElseClean(OUTPUT.toFile());
      if (DETAILS) printDetails();
      if (SUMMARY) printSummary();
    }

    return ALL_RESULTS;
  }

  /**
   * This function inspects the BGP RIB of a non-customer neighbor {@code isp} to find out all BGP
   * routes initiated by another non-customer neighbor in {@code forbids}.
   */
  public static void check(ISP isp, Collection<ISP> forbids) {
    // collect violations
    List<BgpRoute> violations =
        isp.getRib().getAllRoutes().stream()
            .filter(
                route ->
                    route.getPrefixesBdd() != 0
                        && forbids.stream()
                            .anyMatch(
                                forbid ->
                                    (forbid.getAs() != isp.getAs()
                                        && route.getAsPath().containsAs(forbid.getAs()))))
            .collect(Collectors.toList());
    if (!violations.isEmpty()) {
      ALL_RESULTS.put(isp, violations);
    }
  }

  public static void printDetails() {
    ALL_RESULTS.keySet().forEach(GaoRexfordChecker::print);
  }

  public static void print(ISP isp) {
    List<BgpRoute> violations = ALL_RESULTS.get(isp);
    if (!violations.isEmpty()) {
      try {
        RESULT_WRITER =
            new JsonWriter(new FileWriter(OUTPUT.resolve(isp.getName() + ".json").toFile()));
        RESULT_WRITER.setIndent(" ");
        RESULT_WRITER.beginObject();
        RESULT_WRITER.name("violations");
        RESULT_WRITER.beginArray();
        violations.forEach(GaoRexfordChecker::printBgpRoute);
        RESULT_WRITER.endArray();
        RESULT_WRITER.endObject();
        RESULT_WRITER.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void printBgpRoute(BgpRoute bgpRoute) {
    try {
      RESULT_WRITER.beginObject();

      RESULT_WRITER.name("prefixes");
      if (WITH_ENVC) printPrefixRangeWithEnvc(RESULT_WRITER, bgpRoute.getPrefixesBdd());
      else printPrefixRange(RESULT_WRITER, bgpRoute.getPrefixesBdd());

      RESULT_WRITER.name("nextHopIp");
      RESULT_WRITER.value(bgpRoute.getNextHopIp().toString());

      RESULT_WRITER.name("nextHopInterface");
      RESULT_WRITER.value(
          (bgpRoute.getNextHopInterface() == null
              ? null
              : bgpRoute.getNextHopInterface().getInterfaceName().getFullName()));

      RESULT_WRITER.name("localPreference");
      RESULT_WRITER.value(bgpRoute.getLocalPreference());

      RESULT_WRITER.name("path");
      RESULT_WRITER.value(bgpRoute.getPath().toString());

      RESULT_WRITER.name("asPath");
      RESULT_WRITER.value(bgpRoute.getAsPath().toString());

      RESULT_WRITER.name("communities");
      RESULT_WRITER.value(
          bgpRoute.getCommunities() == null ? null : bgpRoute.getCommunities().toString());

      RESULT_WRITER.endObject();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Multimap<PrefixRange, datamodel.path.Path> summarize() {
    Multimap<PrefixRange, datamodel.path.Path> summary = HashMultimap.create();
    ALL_RESULTS.forEach(
        (k, v) ->
            v.forEach(
                bgpRoute ->
                    Controller.bddManager
                        .getBddPrefixWrapper()
                        .extractPrefixRanges(bgpRoute.getPrefixesBdd())
                        .forEach(
                            prefixRange -> {
                              datamodel.path.Path path =
                                  new datamodel.path.Path(bgpRoute.getPath(), k);
                              summary.put(prefixRange, path);
                            })));
    // summary.entries().removeIf(entry ->
    // INTERNAL_PREFIXES.containsAll(entry.getKey().toPrefixes()));
    return summary;
  }

  public static void printSummary() {
    Multimap<PrefixRange, datamodel.path.Path> summary = summarize();
    try {
      RESULT_WRITER =
          new JsonWriter(new FileWriter(OUTPUT.resolve("summary_pernode.json").toFile()));
      RESULT_WRITER.setIndent(" ");

      RESULT_WRITER.beginObject();
      for (Map.Entry<PrefixRange, Collection<datamodel.path.Path>> e : summary.asMap().entrySet()) {
        RESULT_WRITER.name(e.getKey().toString());
        RESULT_WRITER.beginObject();
        int i = 0;
        for (datamodel.path.Path path :
            e.getValue().stream().sorted().collect(Collectors.toList())) {
          RESULT_WRITER.name("" + i++);
          RESULT_WRITER.beginArray();
          for (PathHop hop : path.getHops()) {
            RESULT_WRITER.value(hop.hopString());
          }
          RESULT_WRITER.endArray();
        }
        RESULT_WRITER.endObject();
      }
      RESULT_WRITER.endObject();

      RESULT_WRITER.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
