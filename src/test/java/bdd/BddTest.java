package bdd;

import jdd.bdd.BDD;
import org.junit.Test;
import util.BddUtil;

import static org.junit.Assert.assertEquals;

public class BddTest {
  @Test
  public void testReplace() {
    BDD bdd = new BDD(10000, 1000);
    int[] from = new int[4];
    int[] to = new int[4];
    for (int i = 0; i < 4; i++) {
      from[i] = bdd.createVar();
    }
    for (int i = 0; i < 4; i++) {
      to[i] = bdd.createVar();
    }
    int before = bdd.ref(bdd.and(from[0], bdd.not(from[1])));
    bdd.printDot("figures/bdd/test/before-replace", before);
    int after = bdd.replace(before, bdd.createPermutation(from, to));
    bdd.printDot("figures/bdd/test/after-replace", after);
    assertEquals(after, bdd.ref(bdd.and(to[0], bdd.not(to[1]))));

    before = bdd.or(before, from[2]);
    bdd.printDot("figures/bdd/test/before-replace1", before);
    after = bdd.replace(before, bdd.createPermutation(from, to));
    bdd.printDot("figures/bdd/test/after-replace1", after);
  }

  @Test
  public void testImp() {
    BDD bdd = new BDD(10000, 1000);
    int[] from = new int[1];
    int[] to = new int[4];
    for (int i = 0; i < 1; i++) {
      from[i] = bdd.createVar();
    }
    for (int i = 0; i < 4; i++) {
      to[i] = bdd.createVar();
    }
    int or = BddUtil.orInBatch(bdd, to);
    int imp = bdd.imp(from[0], or);
    int biimp = bdd.biimp(from[0], or);
    bdd.printDot("figures/bdd/dp_test/imp", imp);
    bdd.printDot("figures/bdd/dp_test/biimp", biimp);
  }

  public static int add(BDD bdd, int set, int var) {
    if (set == 0) return 0;
    if (set == 1) return bdd.ref(bdd.mk(var, 0, 1));
    return addRec(bdd, set, var);
  }

  public static int addRec(BDD bdd, int set, int var) {
    int curVar = bdd.getVar(set);
    if (curVar < var) {
      int low = addRec(bdd, bdd.getLow(set), var);
      int high = addRec(bdd, bdd.getHigh(set), var);
      return bdd.ref(bdd.mk(curVar, low, high));
    } else if (curVar == var) {
      int high = bdd.getHigh(set);
      high = high == 0 ? 1 : high;
      return bdd.ref(bdd.mk(var, 0, high));
    } else {
      return bdd.ref(bdd.mk(var, 0, set));
    }
  }

  @Test
  public void testAdd() {
    BDD bdd = new BDD(10000, 1000);
    int[] vars = new int[3];
    for (int i = 0; i < 3; i++) vars[i] = bdd.createVar();

    int set = bdd.ref(bdd.and(vars[0], bdd.not(vars[2])));
    bdd.printDot("figures/bdd/beforeAdd", set);

    set = add(bdd, set, bdd.getVar(vars[1]));
    bdd.printDot("figures/bdd/afterAdd", set);
  }

  @Test
  public void testRestrict() {
    BDD bdd = new BDD(10000, 1000);
    int[] v = new int[3];
    for (int i = 0; i < 3; i++) v[i] = bdd.createVar();

    int a = bdd.and(bdd.and(v[0], v[1]), v[2]);
    int b = bdd.and(bdd.not(v[0]), bdd.and(bdd.not(v[1]), v[2]));
    int c = bdd.and(v[0], bdd.not(v[2]));

    int abc = bdd.or(bdd.or(a, b), c);
    int abc1 = bdd.restrict(abc, v[1]);
    int abc2 = bdd.restrict(abc, bdd.not(v[1]));
    int abc3 = bdd.or(abc1, abc2);

    bdd.printDot("figures/bdd/beforeRestrict", abc);
    bdd.printDot("figures/bdd/afterRestrict", abc3);
  }
}
