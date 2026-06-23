package bdd;

import com.google.common.collect.ImmutableList;
import jdd.zdd.ZDD;
import org.junit.Test;
import util.ZddUtil;

import static org.junit.Assert.assertEquals;

public class ZddTest {
  @Test
  public void testZdd() {
    int size = 10;

    ZDD zdd = new ZDD(10000, 1000);
    int[] vars = new int[size];
    for (int i = 0; i < size; i++) {
      vars[i] = zdd.createVar();
    }

    long start = System.nanoTime();
    int allSingletonSets = ZddUtil.allSingletonSets(zdd, vars);
    System.out.println(
        "time to compute all singleton sets: " + (System.nanoTime() - start) / 1e9 + "s");

    zdd.printDot("figures/zdd_all_singleton_sets", allSingletonSets);

    int ab = ZddUtil.allSingletonSets(zdd, ImmutableList.of(vars[0], vars[1]));
    int bc = ZddUtil.allSingletonSets(zdd, ImmutableList.of(vars[1], vars[2]));
    int b = ZddUtil.allSingletonSets(zdd, ImmutableList.of(vars[1]));
    int abc = ZddUtil.allSingletonSets(zdd, ImmutableList.of(vars[0], vars[1], vars[2]));
    int intersect = zdd.intersect(ab, bc);
    int union = zdd.union(ab, bc);

    zdd.printDot("figures/zdd_intersect", intersect);
    zdd.printDot("figures/zdd_union", union);
    assertEquals(intersect, b);
    assertEquals(union, abc);

    System.out.println(ZddUtil.allSatString(zdd, size, ab));
  }

  @Test
  public void testZddSubset() {
    ZDD zdd = new ZDD(1000, 100);
    int[] vars = new int[3];
    for (int i = 0; i < 3; i++) vars[i] = zdd.createVar();

    int universe = zdd.ref(zdd.universe());

    int a = zdd.ref(zdd.subset1(universe, vars[0]));
    int a1 = zdd.ref(zdd.change(a, vars[0]));
    System.out.println(ZddUtil.allSatString(zdd, 3, a));
    System.out.println(ZddUtil.allSatString(zdd, 3, a1));
    System.out.println();

    int b = zdd.ref(zdd.subset0(universe, vars[1]));
    System.out.println(ZddUtil.allSatString(zdd, 3, b));
    System.out.println();

    int ab = zdd.ref(zdd.intersect(a, b));
    int ab1 = zdd.ref(zdd.intersect(a1, b));
    System.out.println(ZddUtil.allSatString(zdd, 3, ab));
    System.out.println(ZddUtil.allSatString(zdd, 3, ab1));
  }

  @Test
  public void testZddAllSat() {
    int size = 2;
    for (int i = 0; i < 3; i++) {
      ZDD zdd = new ZDD(10000, 1000);
      int[] vars = new int[size];
      for (int j = 0; j < size; j++) vars[j] = zdd.createVar();
      int universe = zdd.ref(zdd.universe());
      long start = System.nanoTime();
      String allSatString = ZddUtil.allSatString(zdd, size, universe);
      System.out.println((System.nanoTime() - start) / 1e9 + ", " + allSatString);
      size *= 10;
    }
  }
}
