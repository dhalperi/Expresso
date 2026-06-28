package util;

import jdd.bdd.BDD;
import main.ExpressoLogger;
import org.junit.Ignore;
import org.junit.Test;

public class BddUtilTest {

    @Test
    public void encodeBitVector2BDDDeprecated() {
        BDD bdd = new BDD(100000000, 10000000);
        int size  = 100;
        byte[] bitVector = new byte[size];
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) {
            bitVector[i] = (byte) (i % 2);
            vars[i] = bdd.createVar();
        }
        TimeUtil timer = new TimeUtil();
        timer.begin();
        int tmp = BddUtil.encodeBitVector2BddVarsSorted(bdd, bitVector, vars);
//        bdd.printDot("figure1", tmp);
        timer.end(ExpressoLogger.LEVEL.INFO, "new method finish");
        bdd.showStats();
        bdd.cleanup();
    }

    @Test
    public void encodeBitVector2BDD() {
        BDD bdd = new BDD(100000000, 10000000);
        int size  = 100;
        byte[] bitVector = new byte[size];
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) {
            bitVector[i] = (byte) (i % 2);
            vars[i] = bdd.createVar();
        }
        TimeUtil timer = new TimeUtil();
        timer.begin();
        int tmp = BddUtil.encodeBitVector2BddVarsUnsorted(bdd, bitVector, vars);
//        bdd.printDot("figure1", tmp);
        timer.end(ExpressoLogger.LEVEL.INFO, "old method finish");
        bdd.showStats();
        bdd.cleanup();
    }

    // Pre-existing failure (unrelated to the Batfish migration): recurses into a StackOverflowError.
    @Ignore("Pre-existing failure: encodeKFailure overflows the stack")
    @Test
    public void encodeKFailure() {
        BDD bdd = new BDD(100000000, 10000000);
        int size = 8000;
        int mf = 1;
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) {
            vars[i] = bdd.createVar();
        }
        long start = System.nanoTime();
        int kFailure = BddUtil.encodeKZeroVarsSorted(bdd, vars, mf);
        System.out.println("finish " + (System.nanoTime() - start) / 1e9 + "s");
        System.out.println(CombinationUtil.numItems(size, mf)); // 100s
    }

    @Test
    public void encodeAtMostKFailure() {
        BDD bdd = new BDD(100000000, 10000000);
        int size = 552;
        int mf = 3;
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) {
            vars[i] = bdd.createVar();
        }
        long start = System.nanoTime();
        BddUtil.encodeAtMostKZeroVarsSorted(bdd, vars, mf);
        System.out.println("finish " + (System.nanoTime() - start) / 1e9 + "s");
    }

    @Test
    public void encodeKOneVarsSorted() {
        BDD bdd = new BDD(10000, 1000);

        int[] varsInc = new int[4];
        for (int i = 0; i < 4; i++) {
            varsInc[i] = bdd.createVar();
        }
        int inc = BddUtil.encodeKOneVarsSorted(bdd, varsInc, 1);
        bdd.printDot("figures/inc", inc);

        int[] varsDec = new int[4];
        for (int i = 3; i >= 0; i--) {
            varsDec[i] = bdd.createVar();
        }
        int dec = BddUtil.encodeKOneVarsSorted(bdd, varsDec, 1);
        bdd.printDot("figures/dec", dec);
    }
}
