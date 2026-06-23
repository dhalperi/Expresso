package atomic;

import jdd.bdd.BDD;
import jdd.util.jre.JREInfo;
import main.ExpressoLogger;
import org.junit.Test;
import util.BddUtil;
import util.TimeUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class BddManagerTest {

    @Test
    public void bddPrintDotTest() {
        BDD bdd = new BDD(200);
        int p1 = bdd.createVar();
        int p2 = bdd.createVar();
        int p3 = bdd.createVar();
        int l1 = bdd.createVar();
        int l2 = bdd.createVar();
        int l3 = bdd.createVar();

        int prefix = bdd.ref(bdd.and(bdd.and(p1, bdd.not(p2)), p3));
        int tc = bdd.ref(bdd.or(bdd.or(bdd.and(l1, l2), bdd.and(l1, l3)), bdd.and(l2, l3)));
        int entry = bdd.ref(bdd.and(prefix, tc));

        assertEquals(2, bdd.toZero(22));
    }

    @Test
    public void MAX_K_BDDTest1() {
        BDD bdd = new BDD(100000000, 10000000);
        int size = 700, k = 2;
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) vars[i] = bdd.createVar();

        TimeUtil timer1 = new TimeUtil();
        timer1.begin();
        int tmp1 = BddUtil.encodeAtMostKZeroVarsSorted(bdd, vars, k);
//        bdd.printDot("figure1", tmp1);
        timer1.end(ExpressoLogger.LEVEL.INFO, "new method finish");
        bdd.showStats();
        bdd.cleanup();
    }

    @Test
    public void MAX_K_BDDTest2() {
        BDD bdd = new BDD(100000000, 10000000);
        int size = 100, k = 3;
        int[] vars = new int[size];
        for (int i = 0; i < size; i++) vars[i] = bdd.createVar();

        TimeUtil timer2 = new TimeUtil();
        timer2.begin();
        int tmp2 = BddUtil.encodeAtMostKZeroVarsUnsorted(bdd, vars, k);
//        bdd.printDot("figure2", tmp2);
        timer2.end(ExpressoLogger.LEVEL.INFO, "old method finish");
        bdd.showStats();
        bdd.cleanup();
    }

    @Test
    public void testReset() throws InterruptedException {
        int size = 700;
        int[] vars = new int[size];

        BDD bdd = new BDD(1000000, 1000000);
        for (int i = 0; i < size; i++) vars[i] = bdd.createVar();
        JREInfo.show();

        bdd = null;
        JREInfo.show();

        bdd = new BDD(1000000, 1000000);
        JREInfo.show();

        System.gc();
        Thread.sleep(10000);
        JREInfo.show();
    }

    @Test
    public void testGetVar() {
        BDD bdd = new BDD(1000, 1000);
        int var = bdd.createVar();
        int nvar = bdd.not(var);
        int v1 = bdd.getVar(var), v2 = bdd.getVar(nvar);
        assertEquals(v1, v2);
    }

//    private int filter_rec(BDD bdd, int i, int k) {
//        int var = bdd.getVar(i);
//        if (i <= 1) {
//            return i;
//        }
//        else {
//            int l = 0, h;
//            if (k < 3) {
//                l = filter_rec(bdd, bdd.getLow(i), k + 1);
//            }
//            h = filter_rec(bdd, bdd.getHigh(i), k);
//            bdd.deref(i);
//            return bdd.ref(bdd.mk(var, l, h));
//        }
//    }

    public HashSet<byte[]> allSat(BDD bdd, int tc, int size) {
        HashSet<byte[]> ret = new HashSet<>();
        if (tc == 0) {

        }
        else if (tc == 1) {
            byte[] one = new byte[size];
            Arrays.fill(one, (byte) -1);
            ret.add(one);
        }
        else {
            int idx = bdd.getVar(tc);
            for (byte[] oneSat : allSat(bdd, bdd.getLow(tc), size)) {
                byte[] down = oneSat.clone();
                down[idx] = 0;
                ret.add(down);
            }
            for (byte[] oneSat : allSat(bdd, bdd.getHigh(tc), size)) {
                byte[] up = oneSat.clone();
                up[idx] = 1;
                ret.add(up);
            }
        }
        return ret;
    }

//    private int filter_rec(BDD bdd, int i, int k) {
//        if (k < 0) {
//            return 0;
//        }
//        if (i <= 1) {
//            return i;
//        }
//        int var = bdd.getVar(i), l, h;
//        l = filter_rec(bdd, bdd.getLow(i), k - 1);
//        h = filter_rec(bdd, bdd.getHigh(i), k);
//        bdd.deref(i);
//        return bdd.ref(bdd.mk(var, l, h));
//    }
//
//    @Test
//    public void testFilter() {
//        BDD bdd = new BDD(10000, 10000);
//        int size = 100;
//        int[] vars = new int[size];
//        for (int i = 0; i < size; i++) {
//            vars[i] = bdd.createVar();
//        }
//        int MFBDD = BDDUtil.encodeAtMostKFailureVarsSorted(bdd, vars, 3);
//        int predicate = BDDUtil.encodeAtMostKFailureVarsSorted(bdd, vars, 5);
//
////        System.out.println("before:");
////        for (byte[] one : allSat(bdd, predicate, size)) {
////            System.out.println(Arrays.toString(one));
////        }
////        bdd.printDot("before_filter.png", predicate);
//
//        long start = System.nanoTime();
//        predicate = filter_rec(bdd, predicate, 3);
////        predicate = bdd.ref(bdd.and(predicate, MFBDD));
//        System.out.println("time: " + (System.nanoTime() - start) / 1000000.0 + "ms");
//
////        System.out.println("after:");
////        for (byte[] one : allSat(bdd, predicate, size)) {
////            System.out.println(Arrays.toString(one));
////        }
////        bdd.printDot("after_filter", predicate);
//    }

    BDD bdd;
    int mf = 2;
    HashSet<Integer> hs;
//    private void traverse(int i, int k) {
//        if (k <= mf) {
//            hs.add(i);
//            if (i > 1) {
//                traverse(bdd.getLow(i), k + 1);
//                traverse(bdd.getHigh(i), k);
//            }
//        }
//    }
//
//    private int filter_rec(int i) {
//        if (hs.contains(i)) {
//            if (i <= 1) return i;
//            int var = bdd.getVar(i);
//            int low = filter_rec(bdd.getLow(i));
//            int high = filter_rec(bdd.getHigh(i));
//            return bdd.mk(var, low, high);
//        }
//        else {
//            bdd.deref(i);
//            return 0;
//        }
//    }

    private int filter_rec(int i, int k) {
        if (k > mf) {
            return 0;
        }
        if (i <= 1) {
            return i;
        }
        int var = bdd.getVar(i), l, h;
        l = filter_rec(bdd.getLow(i), k + 1);
        h = filter_rec(bdd.getHigh(i), k);
        bdd.deref(i);
        return bdd.mk(var, l, h);
    }

    @Test
    public void testFilter() {
        bdd = new BDD(1000, 1000);
        int[] v = new int[5];
        for (int i = 0; i < v.length; i++) {
            v[i] = bdd.createVar();
        }
        int p1 = bdd.ref(bdd.and(v[0], bdd.and(v[1], v[2])));
        int p2 = bdd.ref(bdd.and(v[0], bdd.not(v[3])));
        int p3 = bdd.ref(bdd.or(p1, p2));
        bdd.printDot("before_filter", p3);
//        hs = new HashSet<>();
//        traverse(p3, 0);
//        p3 = bdd.ref(filter_rec(p3));
        p3 = bdd.ref(filter_rec(p3, 0));
        bdd.printDot("after_filter", p3);
    }

    @Test
    public void testBDDMemUse() {
        Runtime rt = Runtime.getRuntime();
        long memUse1 = rt.totalMemory() - rt.freeMemory();
        int[] vec = new int[100000000];
        long memUse2 = rt.totalMemory() - rt.freeMemory();
        System.out.println(((double) memUse2 - memUse1) / (1 << 30) + "Gb");
    }

    @Test
    public void testFattree04() {
        BDD bdd = new BDD(10000, 1000);
        String[][] links = {
                {"A17", "E19"},
                {"A17", "C3"},
                {"A5", "C3"},
                {"A5", "E6"},
                {"A17", "C2"},
                {"A5", "C2"},
                {"A16", "E19"},
                {"A16", "C1"},
                {"A4", "C1"},
                {"A4", "E6"},
                {"A16", "C0"},
                {"A4", "C0"}};
        HashMap<String[], Integer> linkVars = new HashMap<>();
        for (String[] link : links) {
            linkVars.put(link, bdd.createVar());
        }
        int tc1 = bdd.ref(bdd.and(bdd.and(linkVars.get(links[0]), linkVars.get(links[1])), bdd.and(linkVars.get(links[2]), linkVars.get(links[3]))));
        int tc2 = bdd.ref(bdd.and(bdd.and(linkVars.get(links[0]), linkVars.get(links[4])), bdd.and(linkVars.get(links[5]), linkVars.get(links[3]))));
        int tc3 = bdd.ref(bdd.and(bdd.and(linkVars.get(links[6]), linkVars.get(links[7])), bdd.and(linkVars.get(links[8]), linkVars.get(links[9]))));
        int tc4 = bdd.ref(bdd.and(bdd.and(linkVars.get(links[6]), linkVars.get(links[10])), bdd.and(linkVars.get(links[11]), linkVars.get(links[9]))));
        int tc_old = bdd.ref(bdd.or(bdd.or(tc1, tc2), bdd.or(tc3, tc4)));
        int tc_new = bdd.ref(bdd.or(tc2, bdd.or(tc3, tc4)));
        int tc_common = bdd.ref(bdd.and(tc_old, tc_new));
        int tc_inc = bdd.ref(bdd.and(bdd.not(tc_old), tc_new));
        int tc_dec = bdd.ref(bdd.and(bdd.not(tc_new), tc_old));
        bdd.printDot("src/test/java/bdd/tc_old", tc_old);
        bdd.printDot("src/test/java/bdd/tc_new", tc_new);
        bdd.printDot("src/test/java/bdd/tc_common", tc_common);
        bdd.printDot("src/test/java/bdd/tc_inc", tc_inc);
        bdd.printDot("src/test/java/bdd/tc_dec", tc_dec);
    }
}
