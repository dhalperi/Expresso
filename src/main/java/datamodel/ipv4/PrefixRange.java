package datamodel.ipv4;

import atomic.bdd.BddRepresented;
import bdd.BddPrefixWrapper;
import com.fasterxml.jackson.annotation.JsonValue;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.Range;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import javafx.util.Pair;
import main.Controller;
import org.jetbrains.annotations.NotNull;
import util.MathUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class PrefixRange implements BddRepresented, CanBeMatched, Comparable<PrefixRange> {
  Prefix prefix;
  Range<Integer> range;
  /** */
  boolean matchNetwork;

  int bddRep = -1;

  private PrefixRange(Prefix prefix, Range<Integer> range, boolean matchNetwork) {
    this.prefix = prefix;
    this.range = range;
    this.matchNetwork = matchNetwork;
  }

  private static boolean valid(Prefix prefix, Range<Integer> range) {
    return prefix.getPreLength() <= range.getLowerBound() && range.getUpperBound() <= 32;
  }

  public static PrefixRange of(Prefix prefix) {
    return new PrefixRange(prefix, new Range<>(prefix.getPreLength()), true);
  }

  public static PrefixRange of(Prefix prefix, Range<Integer> range) {
    if (valid(prefix, range)) {
      return new PrefixRange(prefix, range, true);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static PrefixRange of(Prefix prefix, Range<Integer> range, boolean matchNetwork) {
    if (valid(prefix, range)) {
      return new PrefixRange(prefix, range, matchNetwork);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static boolean valid(Range<Integer> range) {
    return 0 <= range.getLowerBound() && range.getUpperBound() <= 32;
  }

  public static PrefixRange of(Ip ip, Range<Integer> range) {
    if (valid(range)) {
      return new PrefixRange(Prefix.of(ip, Mask.of(range.getLowerBound())), range, true);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static PrefixRange of(Ip ip, Range<Integer> range, boolean matchNetwork) {
    if (valid(range)) {
      return new PrefixRange(Prefix.of(ip, Mask.of(range.getLowerBound())), range, matchNetwork);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static PrefixRange moreSpecificThan(Prefix prefix) {
    Range<Integer> range = new Range<>(prefix.getPreLength() + 1, 32);
    return new PrefixRange(prefix, range, true);
  }

  public Prefix getPrefix() {
    return prefix;
  }

  public Range<Integer> getRange() {
    return range;
  }

  public boolean isMatchNetwork() {
    return matchNetwork;
  }

  public Collection<Prefix> toPrefixes() {
    //        return IntStream
    //                .rangeClosed(range.getLowerBound(), range.getUpperBound())
    //                .mapToObj(len -> Prefix.of(prefix.getIp(), Mask.of(len)))
    //                .collect(Collectors.toList());

    return IntStream.rangeClosed(range.getLowerBound(), range.getUpperBound())
        .mapToObj(
            len -> {
              List<Long> list =
                  LongStream.range(0, (1L << (len - prefix.getPreLength())))
                      .boxed()
                      .map(add -> prefix.getIp().asLong() + (add << (32 - len)))
                      .collect(Collectors.toList());
              return new Pair<>(len, list);
            })
        .flatMap(
            pair ->
                pair.getValue().stream()
                    .map(ipLong -> Prefix.of(Ip.of(ipLong), Mask.of(pair.getKey()))))
        .distinct()
        .collect(Collectors.toList());

    //        Collection<Prefix> prefixes = new TreeSet<>();
    //        List<Long> ipLongs =
    // LongStream.of(prefix.getIp().asLong()).boxed().collect(Collectors.toList());
    //        for (int len = range.getLowerBound(); len <= range.getUpperBound(); len++) {
    //            for (Long ipLong : ipLongs) {
    //                prefixes.add(Prefix.of(Ip.of(ipLong), Mask.of(len)));
    //            }
    //            if (len < 32) {
    //                int s = ipLongs.size();
    //                for (int i = 0; i < s; i++) {
    //                    ipLongs.add(ipLongs.get(i) + (1L << (31 - len)));
    //                }
    //            }
    //        }
    //        return prefixes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PrefixRange that = (PrefixRange) o;
    return matchNetwork == that.matchNetwork
        && Objects.equals(prefix, that.prefix)
        && Objects.equals(range, that.range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prefix, range, matchNetwork);
  }

  @Override
  @JsonValue
  public String toString() {
    return prefix.toString() + ": " + range.toString();
  }

  @Override
  public int toBdd() {
    return bddRep == -1
        ? (bddRep = Controller.bddManager.getBddPrefixWrapper().encodePrefixRange(this))
        : bddRep;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    int permit = Controller.bddManager.and(route.getPrefixesBdd(), toBdd());
    int deny = Controller.bddManager.minus(route.getPrefixesBdd(), toBdd());
    if (permit != 0) {
      result.getPermitted().add(route.toBuilder().setPrefixesBdd(permit).build());
    }
    if (deny != 0) {
      result.getDenied().add(route.toBuilder().setPrefixesBdd(deny).build());
    }
    return result;
  }

  @Override
  public int compareTo(@NotNull PrefixRange o) {
    return Comparator.comparing(PrefixRange::getPrefix)
        .thenComparing(PrefixRange::getRange)
        .compare(this, o);
  }

  public static HashSet<PrefixRange> bin2PrefixRanges(byte[] bv) {
    Pair<Collection<Prefix>, Collection<Range<Integer>>> pair =
        BddPrefixWrapper.bitVector2PrefixRange(bv);
    Collection<Prefix> prefixes = pair.getKey();
    Collection<Range<Integer>> ranges = MathUtil.combineRanges(pair.getValue());
    return prefixes.stream()
        .flatMap(prefix -> BddPrefixWrapper.getPrefixRanges(prefix, ranges, false).stream())
        .collect(Collectors.toCollection(HashSet::new));
  }
}
