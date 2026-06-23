package datamodel.route;

import controlplane.route.Route;
import org.batfish.datamodel.routing_policy.Result;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RouteFilterResult<R extends Route> {
  final R origin;

  // huawei route policies only use permitted and denied
  RouteSet<R> permitted;
  RouteSet<R> denied;

  // used for nested route policies as in Batfish
  /**
   * Use {@link Integer} to represent {@link org.batfish.datamodel.routing_policy.Result}. <br>
   * The 0th bit of the {@link Integer} represents {@link Result#getExit()}. <br>
   * The 1st bit of the {@link Integer} represents {@link Result#getReturn()}. <br>
   * The 2nd bit of the {@link Integer} represents {@link Result#getFallThrough()}. <br>
   * The 3rd bit of the {@link Integer} represents {@link Result#getBooleanValue()}. <br>
   */
  SortedMap<Integer, RouteSet<R>> results;

  public RouteFilterResult(R origin) {
    this.origin = origin;

    this.permitted = new RouteSet<>();
    this.denied = new RouteSet<>();

    this.results = new TreeMap<>();
    IntStream.range(0, (1 << 4)).forEach(i -> this.results.put(i, new RouteSet<>()));
  }

  public Route getOrigin() {
    return origin;
  }

  public RouteSet<R> getPermitted() {
    return permitted;
  }

  public RouteSet<R> getDenied() {
    return denied;
  }

  public void setPermitted(RouteSet<R> permitted) {
    this.permitted = permitted;
  }

  public void setDenied(RouteSet<R> denied) {
    this.denied = denied;
  }

  public SortedMap<Integer, RouteSet<R>> getResults() {
    return results;
  }

  public SortedMap<Integer, RouteSet<R>> getExit() {
    return results.entrySet().stream()
        .filter(e -> exitTrue(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getReturn() {
    return results.entrySet().stream()
        .filter(e -> !exitTrue(e.getKey()) && returnTrue(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getNonExitReturn() {
    return results.entrySet().stream()
        .filter(e -> !exitTrue(e.getKey()) && !returnTrue(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getNonExitFallThrough() {
    return results.entrySet().stream()
        .filter(e -> !exitTrue(e.getKey()) && !fallThroughTrue(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getBoolean(boolean value) {
    return results.entrySet().stream()
        .filter(e -> booleanTrue(e.getKey()) == value)
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getBooleanNonExit(boolean value) {
    return results.entrySet().stream()
        .filter(e -> !exitTrue(e.getKey()) && booleanTrue(e.getKey()) == value)
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getResults(Result res) {
    return results.entrySet().stream()
        .filter(
            e ->
                res.getExit() == exitTrue(e.getKey())
                    && res.getReturn() == returnTrue(e.getKey())
                    && res.getFallThrough() == fallThroughTrue(e.getKey())
                    && res.getBooleanValue() == booleanTrue(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public SortedMap<Integer, RouteSet<R>> getOtherResults(Set<Integer> set) {
    return results.entrySet().stream()
        .filter(e -> !set.contains(e.getKey()))
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
  }

  public void setReturnFalse() {
    for (int i : results.keySet()) {
      if (returnTrue(i)) {
        int ii = i - RETURN;
        results.get(ii).addAll(results.get(i));
        results.get(i).clear();
      }
    }
  }

  public void merge(SortedMap<Integer, RouteSet<R>> map) {
    map.forEach((k, v) -> results.computeIfAbsent(k, i -> new RouteSet<>()).addAll(v));
  }

  public void merge(RouteFilterResult<R> another) {
    permitted.addAll(another.permitted);
    denied.addAll(another.denied);

    another.results.forEach((k, v) -> results.computeIfAbsent(k, i -> new RouteSet<>()).addAll(v));
  }

  public void finalization() {
    getBoolean(true).values().forEach(permitted::addAll);
    getBoolean(false).values().forEach(denied::addAll);
  }

  public static <R extends Route> RouteFilterResult<R> permitAll(R origin) {
    RouteFilterResult<R> result = new RouteFilterResult<>(origin);
    result.getPermitted().add(Route.clone(origin));
    return result;
  }

  public static <R extends Route> RouteFilterResult<R> denyAll(R origin) {
    RouteFilterResult<R> result = new RouteFilterResult<>(origin);
    result.getDenied().add(Route.clone(origin));
    return result;
  }

  public static <R extends Route> SortedMap<Integer, RouteSet<R>> setReturnFalse(
      SortedMap<Integer, RouteSet<R>> map) {
    return map.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> setReturnFalse(e.getKey()),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                TreeMap::new));
  }

  private static final int EXIT = 1;
  private static final int RETURN = 1 << 1;
  private static final int FALLTHROUGH = 1 << 2;
  private static final int BOOLEAN = 1 << 3;

  private static boolean exitTrue(int i) {
    return (i & EXIT) != 0;
  }

  private static boolean returnTrue(int i) {
    return (i & RETURN) != 0;
  }

  private static boolean fallThroughTrue(int i) {
    return (i & FALLTHROUGH) != 0;
  }

  private static boolean booleanTrue(int i) {
    return (i & BOOLEAN) != 0;
  }

  private static int setReturnFalse(int i) {
    return returnTrue(i) ? i - RETURN : i;
  }
}
