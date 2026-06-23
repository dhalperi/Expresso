package bdd;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import datamodel.Range;
import datamodel.ipv4.*;
import javafx.util.Pair;
import main.Controller;
import util.BddUtil;
import util.MathUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Computes BDD for routes
 *
 * <p>Note: The true, false, bdd variables, the negation of bdd variables: their reference count are
 * already set to maximal, so they will never be garbage collected. And no need to worry about the
 * reference count for them.
 */
public class BddPrefixWrapper extends BddWrapper {

  public static final int prefixIpBits = 32;
  public static final int prefixLengthBits = 6;
  public static final int prefixBits = prefixIpBits + prefixLengthBits;

  int[] prefixIp;
  int[] prefixLength;
  final int prefixLengthCube;
  public final int prefixLengthBound;
  public final int prefixSpaceBdd;

  public BddPrefixWrapper(BddManager manager) {
    super(manager);

    prefixIp = new int[prefixIpBits];
    prefixLength = new int[prefixLengthBits];

    DeclareVars(prefixIp, prefixIpBits);
    DeclareVars(prefixLength, prefixLengthBits);

    prefixLengthCube = BddUtil.andInBatch(bdd, prefixLength);

    prefixLengthBound = encodeLengthRange(new Range<>(0, 32));
    if (Controller.prefixSpace == null || Controller.prefixSpace.isEmpty()) {
      prefixSpaceBdd = prefixLengthBound;
    } else {
      prefixSpaceBdd =
          BddUtil.orInBatch(
              bdd,
              Controller.prefixSpace.stream()
                  .map(this::encodePrefixRange)
                  .collect(Collectors.toSet()));
    }
  }

  /* Following are functions used to encode an Ip/Prefix/PrefixRange into a bdd predicate. */

  int and(int i, int j) {
    return bdd.ref(bdd.and(i, j));
  }

  // we are decreasing idx since vars are in decreasing order
  // e.g. srcIP: [31,30,...,1,0]
  int encode(byte[] binRep, int[] vars, int idx) {
    if (binRep.length > vars.length) {
      throw new IllegalArgumentException();
    }

    if (idx < 0) {
      return 1;
    }

    int l = 0, h = 0, v = bdd.getVar(vars[vars.length - binRep.length + idx]);
    int nxt = encode(binRep, vars, idx - 1);
    assert binRep[idx] != -1;
    if (binRep[idx] == 0) {
      l = nxt;
    }
    if (binRep[idx] == 1) {
      h = nxt;
    }
    return bdd.ref(bdd.mk(v, l, h));
  }

  int encodePrefixIp(byte[] ip) {
    return encode(ip, prefixIp, ip.length - 1);
  }

  int encodePrefixLength(byte[] length) {
    return encode(length, prefixLength, length.length - 1);
  }

  HashMap<Ip, Integer> ipMemo = new HashMap<>();
  /** Encode an IP {@param ip} as a bdd formula. */
  public int encodeIp(Ip ip) {
    if (!ipMemo.containsKey(ip)) {
      ipMemo.put(ip, encodePrefixIp(MathUtil.calBinRep(ip.asLong(), prefixIpBits)));
    }
    return ipMemo.get(ip);
  }

  HashMap<Integer, Integer> lengthMemo = new HashMap<>();
  /** Encode a prefix length {@param length} as a bdd formula. */
  public int encodeLength(int length) {
    assert length >= 0 && length <= 32;
    if (!lengthMemo.containsKey(length)) {
      lengthMemo.put(length, encodePrefixLength(MathUtil.calBinRep(length, prefixLengthBits)));
    }
    return lengthMemo.get(length);
  }

  HashMap<Prefix, Integer> prefixMemo = new HashMap<>();
  /** Encode a prefix {@param prefix} as a bdd formula. */
  public int encodePrefix(Prefix prefix) {
    if (!prefixMemo.containsKey(prefix)) {
      int ip = encodeIpWildcard(prefix.getIp(), prefix.getPreLength());
      int length = encodeLength(prefix.getPreLength());
      prefixMemo.put(prefix, and(ip, length));
    }
    return prefixMemo.get(prefix);
  }

  /** Encode a collection of prefixes {@param prefixes} as a bdd formula. */
  public int encodePrefixes(Collection<Prefix> prefixes) {
    Set<Integer> tmp = prefixes.stream().map(this::encodePrefix).collect(Collectors.toSet());
    return BddUtil.orInBatch(bdd, tmp);
  }

  HashMap<Range<Integer>, Integer> rangeLengthMemo = new HashMap<>();
  /** Encode a prefix length range {@param range} as a bdd formula. */
  public int encodeLengthRange(Range<Integer> range) {
    assert range.getLowerBound() >= 0 && range.getUpperBound() <= 32;
    if (!rangeLengthMemo.containsKey(range)) {
      LinkedList<byte[]> intervals = MathUtil.decomposeInterval(range, prefixLengthBits);
      int tmp = 0;
      for (byte[] interval : intervals) {
        tmp = bdd.or(tmp, encodePrefixLength(interval));
      }
      rangeLengthMemo.put(range, bdd.ref(tmp));
    }
    return rangeLengthMemo.get(range);
  }

  public int encodeIpWildcard(Ip ip, int length) {
    byte[] ipBin = MathUtil.calBinRep(ip.asLong(), prefixIpBits);
    ipBin = Arrays.copyOfRange(ipBin, prefixIpBits - length, prefixIpBits);
    return encodePrefixIp(ipBin);
  }

  HashMap<PrefixRange, Integer> prefixRangeMemo = new HashMap<>();
  /** Encode a prefix range {@param prefixRange} as a bdd formula. */
  public int encodePrefixRange(PrefixRange prefixRange) {
    if (!prefixRangeMemo.containsKey(prefixRange)) {
      int range = encodeLengthRange(prefixRange.getRange());
      if (prefixRange.isMatchNetwork()) {
        int ipWildcard =
            encodeIpWildcard(
                prefixRange.getPrefix().getIp(), prefixRange.getPrefix().getPreLength());
        prefixRangeMemo.put(prefixRange, and(ipWildcard, range));
      } else {
        prefixRangeMemo.put(prefixRange, range);
      }
    }
    return prefixRangeMemo.get(prefixRange);
  }

  /* Following are functions used to extract PrefixRanges from a bdd predicate. */

  /**
   * @param memo memo to cache result items
   * @param collectorGenerator function to generate collector
   * @param trans function to transform bit vector to collector items
   * @param resultGenerator function to transform collector items to result items
   * @param predicate the bdd predicate
   * @param <R> type of result
   * @param <C> type of collector
   * @return 1 if hit the memo else 0
   */
  private <R, C> int helper(
      HashMap<Integer, R> memo,
      Supplier<C> collectorGenerator,
      BiConsumer<C, Pair<byte[], Integer>> trans,
      Function<C, R> resultGenerator,
      int predicate) {
    if (!memo.containsKey(predicate)) {
      // recursive collect
      C collector = collectorGenerator.get();
      byte[] bv = new byte[prefixBits];
      Arrays.fill(bv, (byte) -1);
      helperRec(trans, collector, bv, predicate);
      // generate result and put into memo
      R result = resultGenerator.apply(collector);
      memo.put(predicate, result);
      return 0;
    } else {
      // hit memo
      return 1;
    }
  }

  private <C> void helperRec(
      BiConsumer<C, Pair<byte[], Integer>> trans, C collector, byte[] bv, int predicate) {
    if (predicate != BddManager.BDDFalse) {
      int var = bdd.getVar(predicate);
      if (var >= prefixBits) {
        trans.accept(collector, new Pair<>(bv, predicate));
      } else {
        int idx = prefixBits - 1 - var;
        bv[idx] = 0;
        helperRec(trans, collector, bv, bdd.getLow(predicate));
        bv[idx] = 1;
        helperRec(trans, collector, bv, bdd.getHigh(predicate));
        bv[idx] = -1;
      }
    }
  }

  HashMap<Integer, HashMap<PrefixRange, Integer>> splitMemo = new HashMap<>();
  int splitMemoHit = 0;
  /** split a bdd predicate into {@link PrefixRange} and environment condition (ENVC) pairs */
  public HashMap<PrefixRange, Integer> splitPrefixRangeEnvc(int predicate) {
    splitMemoHit +=
        helper(
            splitMemo,
            HashMultimap::create,
            (collector, pair) -> {
              Pair<Collection<Prefix>, Collection<Range<Integer>>> tmp =
                  bitVector2PrefixRange(pair.getKey());
              tmp.getKey()
                  .forEach(
                      prefix ->
                          collector.putAll(new Pair<>(prefix, pair.getValue()), tmp.getValue()));
            },
            (Function<
                    Multimap<Pair<Prefix, Integer>, Range<Integer>>, HashMap<PrefixRange, Integer>>)
                collector -> {
                  HashMap<PrefixRange, Integer> result = new HashMap<>();
                  collector
                      .asMap()
                      .forEach(
                          (pair, ranges) -> {
                            Prefix prefix = pair.getKey();
                            int envc = pair.getValue();
                            getPrefixRanges(prefix, ranges, true)
                                .forEach(prefixRange -> result.put(prefixRange, envc));
                          });
                  return result;
                },
            predicate);
    return splitMemo.get(predicate);
  }

  HashMap<Integer, Integer> eraseMemo = new HashMap<>();
  int eraseMemoHit = 0;

  public int eraseEnvc(int predicate) {
    eraseMemoHit +=
        helper(
            eraseMemo,
            HashSet::new,
            (collector, pair) -> {
              byte[] bv = pair.getKey();
              int ip =
                  BddUtil.encodeBitVector2BddVarsSorted(
                      bdd, Arrays.copyOfRange(bv, prefixLengthBits, prefixBits), prefixIp);
              int length =
                  BddUtil.encodeBitVector2BddVarsSorted(
                      bdd, Arrays.copyOfRange(bv, 0, prefixLengthBits), prefixLength);
              collector.add(and(ip, length));
            },
            (Function<HashSet<Integer>, Integer>) collector -> BddUtil.orInBatch(bdd, collector),
            predicate);
    return eraseMemo.get(predicate);
  }

  HashMap<Integer, SortedSet<PrefixRange>> memo = new HashMap<>();
  int memoHit = 0;
  /** extract {@link PrefixRange}s from a bdd predicate */
  public SortedSet<PrefixRange> extractPrefixRanges(int predicate) {
    memoHit +=
        helper(
            memo,
            HashMultimap::create,
            (collector, pair) -> {
              Pair<Collection<Prefix>, Collection<Range<Integer>>> tmp =
                  bitVector2PrefixRange(pair.getKey());
              tmp.getKey().forEach(prefix -> collector.putAll(prefix, tmp.getValue()));
            },
            (Function<Multimap<Prefix, Range<Integer>>, SortedSet<PrefixRange>>)
                collector -> {
                  TreeSet<PrefixRange> result = new TreeSet<>();
                  collector
                      .asMap()
                      .forEach(
                          (prefix, ranges) -> result.addAll(getPrefixRanges(prefix, ranges, true)));
                  return result;
                },
            predicate);
    return memo.get(predicate);
  }

  public SortedSet<PrefixRange> extractPrefixRangesUsingBitTrie(int predicate) {
    memoHit +=
        helper(
            memo,
            HashMultimap::create,
            (collector, pair) -> {
              byte[] ipBits = Arrays.copyOfRange(pair.getKey(), prefixLengthBits, prefixBits);
              byte[] lengthBits = Arrays.copyOfRange(pair.getKey(), 0, prefixLengthBits);
              collector.putAll(ipBits, MathUtil.bitVector2Range(lengthBits, 32));
            },
            (Function<HashMultimap<byte[], Range<Integer>>, SortedSet<PrefixRange>>)
                collector -> {
                  BitTrie bitTrie = new BitTrie();
                  collector.asMap().forEach(bitTrie::add);
                  return new TreeSet<>(bitTrie.collectPrefixRanges());
                },
            predicate);
    return memo.get(predicate);
  }

  public PrefixRange extractOnePrefixRange(int predicate) {
    int[] oneSat = new int[prefixBits];
    bdd.oneSat(predicate, oneSat);

    byte[] bv = new byte[prefixBits];
    for (int i = 0; i < prefixLengthBits; i++) {
      bv[i] = (byte) oneSat[i];
    }
    int i = prefixBits - 1;
    while (i >= prefixLengthBits && oneSat[i] == -1) {
      bv[i] = (byte) oneSat[i];
      i--;
    }
    while (i >= prefixLengthBits) {
      bv[i] = (byte) (oneSat[i] == -1 ? 0 : oneSat[i]);
      i--;
    }

    Pair<Collection<Prefix>, Collection<Range<Integer>>> pair = bitVector2PrefixRange(bv);
    return PrefixRange.of(pair.getKey().iterator().next(), pair.getValue().iterator().next());
  }

  public static Pair<Collection<Prefix>, Collection<Range<Integer>>> bitVector2PrefixRange(
      byte[] bv) {
    Collection<Prefix> prefixes =
        Ip.ipBinToPrefixes(Arrays.copyOfRange(bv, prefixLengthBits, prefixBits));
    Collection<Range<Integer>> ranges =
        MathUtil.bitVector2Range(Arrays.copyOfRange(bv, 0, prefixLengthBits), 32);
    return new Pair<>(prefixes, ranges);
  }

  public static Set<PrefixRange> getPrefixRanges(
      Prefix prefix, Collection<Range<Integer>> ranges, boolean combine) {
    return (combine ? MathUtil.combineRanges(ranges) : ranges)
        .stream()
            .flatMap(
                range -> {
                  if (prefix.getPreLength() <= range.getLowerBound()) {
                    /* prefixLength < rangeLowerBound <= rangeUpperBound */
                    return Stream.of(PrefixRange.of(prefix, range));
                  } else if (prefix.getPreLength() <= range.getUpperBound()) {
                    /* rangeLowerBound < prefixLength <= rangeUpperBound */
                    return Stream.concat(
                        IntStream.range(range.getLowerBound(), prefix.getPreLength())
                            .mapToObj(
                                len ->
                                    PrefixRange.of(
                                        Prefix.of(prefix.getIp(), Mask.of(len)), new Range<>(len))),
                        Stream.of(
                            PrefixRange.of(
                                prefix,
                                new Range<>(prefix.getPreLength(), range.getUpperBound()))));
                  } else {
                    /* rangeLowerBound <= rangeUpperBound < prefixLength */
                    return IntStream.rangeClosed(range.getLowerBound(), range.getUpperBound())
                        .mapToObj(
                            len ->
                                PrefixRange.of(
                                    Prefix.of(prefix.getIp(), Mask.of(len)), new Range<>(len)));
                  }
                })
            .collect(Collectors.toSet());
  }

  HashMap<Integer, SortedSet<Prefix>> extractPrefixMemo = new HashMap<>();
  int extractPrefixMemoHit = 0;
  /** extract {@link Prefix}s from a bdd packet */
  public SortedSet<Prefix> extractPrefixesFromPacket(int pkt) {
    extractPrefixMemoHit +=
        helper(
            extractPrefixMemo,
            TreeSet::new,
            (collector, pair) -> {
              byte[] ipBits = Arrays.copyOfRange(pair.getKey(), prefixLengthBits, prefixBits);
              collector.addAll(Ip.ipBinToPrefixes(ipBits));
            },
            (Function<SortedSet<Prefix>, SortedSet<Prefix>>) collector -> collector,
            pkt);
    return extractPrefixMemo.get(pkt);
  }

  public int eraseLength(int predicate) {
    return bdd.ref(bdd.exists(predicate, prefixLengthCube));
  }

  int[] ipCubes = new int[33];

  int getIpCube(int length) {
    if (ipCubes[length] == 0) {
      int[] tmp = new int[33 - length];
      for (int i = 0; i + length < 32; i++) {
        tmp[i] = prefixIp[i];
      }
      tmp[tmp.length - 1] = prefixLengthCube;
      ipCubes[length] = BddUtil.andInBatch(bdd, tmp);
    }
    return ipCubes[length];
  }

  public int eraseLength(int predicate, int length) {
    int cube = getIpCube(length);
    return bdd.ref(bdd.exists(predicate, cube));
  }

  int envcCube = -1;

  int getEnvcCube() {
    return envcCube != -1
        ? envcCube
        : (envcCube =
            BddUtil.andInBatch(
                bdd,
                manager.getBddEnvcWrapper().ispVars.values().stream()
                    .map(ispVar -> ispVar.cpVar)
                    .collect(Collectors.toSet())));
  }

  public int eraseEnvcUsingExists(int predicate) {
    return bdd.ref(bdd.exists(predicate, getEnvcCube()));
  }

  public int eraseAll(int predicate) {
    return bdd.ref(bdd.exists(predicate, bdd.and(prefixLengthCube, getEnvcCube())));
  }
}
