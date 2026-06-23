package bdd;

import controlplane.process.bgp.ISP;
import jdd.bdd.BDD;
import jdd.bdd.NodeTableStats;
import main.Configuration;
import main.Controller;
import main.ExpressoLogger;
import util.BddUtil;
import util.TimeUtil;

import java.util.HashMap;
import java.util.HashSet;

public class BddManager {
    int ntSize;
    int cacheSize;

    BDD bdd;
    BddPrefixWrapper bddPrefixWrapper;
    BddEnvcWrapper bddEnvcWrapper;

    public final static int BDDFalse = 0;
    public final static int BDDTrue = 1;

    public BddManager() {
        this(Configuration.DEFAULT_BDD_NT_SIZE, Configuration.DEFAULT_BDD_CACHE_SIZE);
    }

    /**
     * we must create ACL wrapper first and then TC wrapper,
     * in order to create prefix related bdd vars first
     */
    public BddManager(int nt, int cache) {
        ExpressoLogger.pushContext("BDD INIT");

        TimeUtil timer = new TimeUtil();
        timer.begin();

        ntSize = nt;
        cacheSize = cache;
        bdd = new BDD(nt, cache);
        bddPrefixWrapper = new BddPrefixWrapper(this);
        bddEnvcWrapper = new BddEnvcWrapper(this);

        timer.end(ExpressoLogger.LEVEL.INFO, "");

        ExpressoLogger.popContext();
    }

    public BDD getBDD() {
        return bdd;
    }

    public BddPrefixWrapper getBddPrefixWrapper() {
        return bddPrefixWrapper;
    }

    public BddEnvcWrapper getBddEnvcWrapper() {
        return bddEnvcWrapper;
    }

    /* wrapper functions of jdd operations. */

    /** Do not use it in data plane computation! */
    public int and(int i, int j) {
        return bdd.ref(bdd.and(bddPrefixWrapper.prefixLengthBound, bdd.and(i, j)));
    }

    /** Use it in data plane computation! */
    public int and1(int i, int j) {
        return bdd.ref(bdd.and(i, j));
    }

    /** Do not use it in data plane computation! */
    public int or(int i, int j) {
        return bdd.ref(bdd.and(bddPrefixWrapper.prefixLengthBound, bdd.or(i, j)));
    }

    /** Use it in data plane computation! */
    public int or1(int i, int j) {
        return bdd.ref(bdd.or(i, j));
    }

    /** Do not use it in data plane computation! */
    public int not(int i) {
        return bdd.ref(bdd.and(bddPrefixWrapper.prefixLengthBound, bdd.not(i)));
    }

    /** Use it in data plane computation! */
    public int not1(int i) {
        return bdd.ref(bdd.not(i));
    }

    /** Do not use it in data plane computation! */
    public int minus(int i, int j) {
//        return bdd.andTo(not(j), i);
        return and(not(j), i);
    }

    /** Use it in data plane computation! */
    public int minus1(int i, int j) {
//        return bdd.andTo(bdd.not(j), i);
        return and(not1(j), i);
    }

    public int ref(int i) {
        return bdd.ref(i);
    }

    public void deref(int i) {
        bdd.deref(i);
    }

    private Integer internalPrefixesBdd;
    public int generateSymbolicRouteForISP(ISP isp) {
        int tmp;
        if (Controller.cpExecutor.bgpTopology.getInternalASNs().contains(isp.getAs())) {
            if (internalPrefixesBdd == null) {
                internalPrefixesBdd = bddPrefixWrapper.encodePrefixes(Controller.cpExecutor.bgpTopology.getInternalPrefixes());
            }
            tmp = BddUtil.andInBatch(
                    bdd,
                    bddPrefixWrapper.prefixSpaceBdd,
                    bddEnvcWrapper.ispVars.get(isp).cpVar,
                    internalPrefixesBdd
            );
        }
        else {
            tmp = and(bddPrefixWrapper.prefixSpaceBdd, bddEnvcWrapper.ispVars.get(isp).cpVar);
        }
        return tmp;
    }

    HashMap<Integer, HashSet<byte[]>> bvMemo = new HashMap<>();
    int bvMemoHit = 0;
    /**
     * each bit vector returned:
     *     [isp_n, ..., isp_0, l_5, l_4, ..., l_0, p_31, p_30, ..., p_0]
     */
    public HashSet<byte[]> getBitVectors(int predicate) {
        if (!bvMemo.containsKey(predicate)) {
            bvMemo.put(predicate, BddUtil.allSat(bdd, predicate));
        }
        else {
            bvMemoHit++;
        }
        return bvMemo.get(predicate);
    }

    /**
     * @return the size of bdd (in bytes)
     */
    public long getBddSize() {
        return bdd.getMemoryUsage();
    }

    public void showStats() {
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        bdd.showStats();
        System.out.println("-------------------------------------------------------------------------------------------------------------");
    }

    public void logUsage() {
        /* log bdd node table usage */
        ExpressoLogger.pushContext("BDD USAGE");
        bdd.gc();
        NodeTableStats stats = bdd.getStats();
        ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, stats.get_table_size() + " " + stats.get_free_nodes_count());
        ExpressoLogger.popContext();
    }

    public void logNumVars() {
        ExpressoLogger.pushContext("BDD VAR");
        ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "" + bdd.numberOfVariables());
        ExpressoLogger.popContext();
    }

    public void clean() {
        bdd.cleanup();
    }
}
