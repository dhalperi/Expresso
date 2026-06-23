package bdd;

import atomic.bdd.BddAtomicPredicates;
import datamodel.Range;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import jdd.bdd.BDD;
import junit.framework.TestCase;
import main.Controller;
import util.BddUtil;
import util.MathUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BddPrefixWrapperTest extends TestCase {

  public void testEncodePrefixRange() {
    Controller.bddManager = new BddManager();

    Prefix prefix = Prefix.of("11.252.0.0/19");
    Range<Integer> range1 = new Range<>(19, 19);
    Range<Integer> range2 = new Range<>(19, 24);
    Range<Integer> range3 = new Range<>(19, 32);
    PrefixRange pr1 = PrefixRange.of(prefix, range1);
    PrefixRange pr2 = PrefixRange.of(prefix, range2);
    PrefixRange pr3 = PrefixRange.of(prefix, range3);

    Set<PrefixRange> set = new HashSet<>();
    Collections.addAll(set, pr1, pr2, pr3);

    BddAtomicPredicates<PrefixRange> aps = new BddAtomicPredicates<>(set);
  }

  public void testBitVector2Range() {
    byte[] bv = new byte[] {-1, -1, -1, -1};
    Collection<Range<Integer>> ranges = MathUtil.bitVector2Range(bv);
    assertEquals(ranges.size(), 1);
    assertEquals(ranges.iterator().next(), new Range<>(0, 15));

    bv = new byte[] {0, 1, 0, 1};
    ranges = MathUtil.bitVector2Range(bv);
    assertEquals(ranges.size(), 1);
    assertEquals(ranges.iterator().next(), new Range<>(10, 10));

    bv = new byte[] {-1, 0, 1, 0};
    ranges = MathUtil.bitVector2Range(bv);
    assertEquals(ranges.size(), 1);
    assertEquals(ranges.iterator().next(), new Range<>(4, 5));

    BddManager manager = new BddManager();
    Collection<PrefixRange> collection =
        manager.bddPrefixWrapper.extractPrefixRanges(manager.bddPrefixWrapper.prefixLengthBound);
  }

  public void testEncodePrefix() {
    BddManager manager = new BddManager();
    Prefix prefix = Prefix.of("71.29.49.32/17");
    int prefixBdd = manager.bddPrefixWrapper.encodePrefix(prefix);
    Collection<PrefixRange> prefixRanges = manager.bddPrefixWrapper.extractPrefixRanges(prefixBdd);
    assertEquals(prefixRanges.size(), 1);
    assertEquals(prefixRanges.iterator().next(), PrefixRange.of(prefix, new Range<>(17, 17)));
  }

  public void testBddPrefix() {
    BddManager manager = new BddManager();
    Prefix prefix1 = Prefix.of("128.0.0.0/1");
    Prefix prefix2 = Prefix.of("192.0.0.0/2");
    Prefix prefix3 = Prefix.of("128.0.0.0/2");
    int prefixBdd1 = manager.bddPrefixWrapper.encodePrefix(prefix1);
    int prefixBdd2 = manager.bddPrefixWrapper.encodePrefix(prefix2);
    int prefixBdd3 = manager.bddPrefixWrapper.encodePrefix(prefix3);
    int tmp = manager.minus(prefixBdd1, prefixBdd2);
    Set<PrefixRange> set = manager.bddPrefixWrapper.extractPrefixRanges(tmp);
    assertEquals(set.size(), 1);
    assertEquals(set.iterator().next(), PrefixRange.of(prefix1, new Range<>(1, 1)));
  }

  public void testEraseEnvc() {
    BddManager manager = new BddManager();
    Prefix prefix1 = Prefix.of("128.0.0.0/1");
    Prefix prefix2 = Prefix.of("192.0.0.0/2");
    int prefixBdd1 = manager.bddPrefixWrapper.encodePrefix(prefix1);
    int prefixBdd2 = manager.bddPrefixWrapper.encodePrefix(prefix2);
    int isp1 = manager.getBDD().createVar();
    int isp2 = manager.getBDD().createVar();
    int prefixBddWithEnvc =
        manager.or(manager.and(prefixBdd1, isp1), manager.and(prefixBdd2, isp2));
    assertEquals(
        manager.or(prefixBdd1, prefixBdd2), manager.bddPrefixWrapper.eraseEnvc(prefixBddWithEnvc));
  }

  public void testEraseLength() {
    BDD bdd = new BDD(1000, 1000);
    int p1 = bdd.createVar();
    int p2 = bdd.createVar();
    int l1 = bdd.createVar();
    int l2 = bdd.createVar();
    int pr = bdd.ref(bdd.and(bdd.and(p1, p2), bdd.and(l1, bdd.not(l2))));
    bdd.printDot("figures/bdd/test/before-erase", pr);
    int p = bdd.exists(pr, l1);
    bdd.printDot("figures/bdd/test/after-erase1", p);
    p = bdd.exists(p, l2);
    bdd.printDot("figures/bdd/test/after-erase2", p);
    p = bdd.exists(pr, bdd.and(l1, l2));
    bdd.printDot("figures/bdd/test/after-erase", p);
  }

  public void testEncodePrefixSpace() {
    BDD bdd = new BDD(1000, 1000);
    int[] prefixIp = new int[3];
    int[] prefixLength = new int[2];

    for (int i = 2; i >= 0; i--) prefixIp[i] = bdd.createVar();
    for (int i = 1; i >= 0; i--) prefixLength[i] = bdd.createVar();

    int tmp = 0;
    int ipBdd = 1;
    for (int len = 3; len >= 0; len--) {
      byte[] lenBin = MathUtil.calBinRep(len, 2);
      System.out.printf(
          "decimal length:%d, binary length:%s\n",
          len, String.valueOf(lenBin[0]) + String.valueOf(lenBin[1]));
      int lenBdd = BddUtil.encodeBitVector2BddVarsSorted(bdd, lenBin, prefixLength);

      int prefixBdd = bdd.ref(bdd.and(ipBdd, lenBdd));
      bdd.printDot("figures/bdd/test/length" + len, prefixBdd);

      tmp = bdd.or(tmp, bdd.and(ipBdd, lenBdd));

      if (len > 0) ipBdd = bdd.and(ipBdd, bdd.not(prefixIp[3 - len]));
    }
    bdd.printDot("figures/bdd/test/prefixSpace", tmp);
    bdd.printDot(
        "figures/bdd/test/prefixSpaceAndLength0",
        bdd.and(tmp, bdd.and(bdd.not(prefixLength[0]), bdd.not(prefixLength[1]))));
  }
}
