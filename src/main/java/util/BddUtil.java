package util;

import bdd.BddManager;
import jdd.bdd.BDD;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class BddUtil {

  public static int andInBatch(BDD bdd, Integer... predicates) {
    return andInBatch(bdd, Arrays.asList(predicates));
  }

  public static int andInBatch(BDD bdd, Collection<Integer> set) {
    int[] bddNodes = new int[set.size()];
    int i = 0;
    for (int j : set) {
      bddNodes[i++] = j;
    }
    return andInBatch(bdd, bddNodes);
  }

  /**
   * @param bddNodes an array of bdd nodes
   * @return the bdd node which is the AND of all input nodes all temporary nodes are de-referenced.
   *     the input nodes are not de-referenced.
   */
  public static int andInBatch(BDD bdd, int[] bddNodes) {
    int tempNode = BddManager.BDDTrue;
    for (int i = 0; i < bddNodes.length; i++) {
      if (i == 0) {
        tempNode = bddNodes[i];
        bdd.ref(tempNode);
      } else {
        if (bddNodes[i] == BddManager.BDDTrue) {
          // short cut, TRUE does not affect anything
          continue;
        }
        if (bddNodes[i] == BddManager.BDDFalse) {
          // short cut, once FALSE, the result is false
          // the current tempNode is useless now
          bdd.deref(tempNode);
          tempNode = BddManager.BDDFalse;
          break;
        }
        int tempNode2 = bdd.and(tempNode, bddNodes[i]);
        bdd.ref(tempNode2);
        // do not need current tempNode
        bdd.deref(tempNode);
        // refresh
        tempNode = tempNode2;
      }
    }
    return tempNode;
  }

  public static int orInBatch(BDD bdd, Integer... predicates) {
    return orInBatch(bdd, Arrays.asList(predicates));
  }

  public static int orInBatch(BDD bdd, Collection<Integer> set) {
    int[] bddNodes = new int[set.size()];
    int i = 0;
    for (int j : set) {
      bddNodes[i++] = j;
    }
    return orInBatch(bdd, bddNodes);
  }

  public static int orInBatchReverse(BDD bdd, Collection<Integer> set) {
    int[] bddNodes = new int[set.size()];
    int i = set.size() - 1;
    for (int j : set) {
      bddNodes[i--] = j;
    }
    return orInBatch(bdd, bddNodes);
  }

  /**
   * @param bddNodes an array of bdd nodes
   * @return the bdd node which is the OR of all input nodes all temporary nodes are de-referenced.
   *     the input nodes are not de-referenced.
   */
  public static int orInBatch(BDD bdd, int[] bddNodes) {
    int tempNode = BddManager.BDDFalse;
    for (int i = 0; i < bddNodes.length; i++) {
      if (i == 0) {
        tempNode = bddNodes[i];
        bdd.ref(tempNode);
      } else {
        if (bddNodes[i] == BddManager.BDDFalse) {
          // short cut, FALSE does not affect anything
          continue;
        }
        if (bddNodes[i] == BddManager.BDDTrue) {
          // short cut, once TRUE, the result is true
          // the current tempNode is useless now
          bdd.deref(tempNode);
          tempNode = BddManager.BDDTrue;
          break;
        }
        int tempNode2 = bdd.or(tempNode, bddNodes[i]);
        bdd.ref(tempNode2);
        // do not need current tempNode
        bdd.deref(tempNode);
        // refresh
        tempNode = tempNode2;
      }
    }
    return tempNode;
  }

  public static void derefInBatch(BDD bdd, Collection<Integer> hs) {
    for (int var : hs) {
      bdd.deref(var);
    }
  }

  /**
   * @param vars a list of bdd nodes that we do not need anymore
   */
  public static void derefInBatch(BDD bdd, int[] vars) {
    for (int var : vars) {
      bdd.deref(var);
    }
  }

  private static boolean inc(int[] vars) {
    return vars.length >= 2 && vars[1] > vars[0];
  }

  public static int encodeBitVector2BddVarsUnsorted(BDD bdd, byte[] bitVector, int[] vars) {
    if (bitVector.length > vars.length) {
      throw new IllegalArgumentException();
    }
    int ret = BddManager.BDDTrue;
    for (int i = 0; i < bitVector.length; i++) {
      if (bitVector[i] == 1) {
        int tmp = bdd.ref(bdd.and(ret, vars[i]));
        bdd.deref(ret);
        ret = tmp;
      } else if (bitVector[i] == 0) {
        int tmp = bdd.ref(bdd.and(ret, bdd.not(vars[i])));
        bdd.deref(ret);
        ret = tmp;
      }
    }
    return ret;
  }

  /* This method use less time and memory comparing to the unsorted method */
  public static int encodeBitVector2BddVarsSorted(BDD bdd, byte[] bitVector, int[] vars) {
    if (bitVector.length > vars.length) {
      throw new IllegalArgumentException();
    }
    return inc(vars)
        ? bdd.ref(encodeBitVector2BddVarsSortedInc(bdd, bitVector, vars, 0))
        : bdd.ref(encodeBitVector2BddVarsSortedDec(bdd, bitVector, vars, bitVector.length - 1));
  }

  /**
   * For example: <br>
   * bitVector = [1, 1, 0, -1, 0] <br>
   * vars = [v0, v1, v2, v3, v4, v5] <br>
   * result = v0 & v1 & !v2 & !v4
   */
  public static int encodeBitVector2BddVarsSortedInc(
      BDD bdd, byte[] bitVector, int[] vars, int idx) {
    if (idx >= bitVector.length) {
      return 1;
    }
    int l = 0, h = 0, v = bdd.getVar(vars[idx]);
    int nxt = encodeBitVector2BddVarsSortedInc(bdd, bitVector, vars, idx + 1);
    if (bitVector[idx] == -1) {
      return nxt;
    }
    if (bitVector[idx] == 0) {
      l = nxt;
    }
    if (bitVector[idx] == 1) {
      h = nxt;
    }
    return bdd.ref(bdd.mk(v, l, h));
  }

  /**
   * For example: <br>
   * bitVector = [1, 1, 0, -1, 0] <br>
   * vars = [v5, v4, v3, v2, v1, v0] <br>
   * result = !v0 & !v2 & v3 & v4
   */
  public static int encodeBitVector2BddVarsSortedDec(
      BDD bdd, byte[] bitVector, int[] vars, int idx) {
    if (idx < 0) {
      return 1;
    }
    int l = 0, h = 0, v = bdd.getVar(vars[vars.length - bitVector.length + idx]);
    int nxt = encodeBitVector2BddVarsSortedDec(bdd, bitVector, vars, idx - 1);
    if (bitVector[idx] == -1) {
      return nxt;
    }
    if (bitVector[idx] == 0) {
      l = nxt;
    }
    if (bitVector[idx] == 1) {
      h = nxt;
    }
    return bdd.ref(bdd.mk(v, l, h));
  }

  public static int encodeAtMostKZeroVarsUnsorted(BDD bdd, int[] vars, int k) {
    byte[] state = new byte[vars.length];
    Arrays.fill(state, (byte) 1);
    HashSet<Integer> hs = new HashSet<>();
    encodeAtMostKZeroVarsUnsorted(bdd, vars, state, 0, 0, k, hs);
    int ret = orInBatch(bdd, hs);
    derefInBatch(bdd, hs);
    return ret;
  }

  private static void encodeAtMostKZeroVarsUnsorted(
      BDD bdd, int[] vars, byte[] state, int idx, int cnt, int k, HashSet<Integer> hs) {
    if (cnt > k) {
      return;
    }
    if (idx == vars.length) {
      hs.add(encodeBitVector2BddVarsUnsorted(bdd, state, vars));
      return;
    }
    state[idx] = 0;
    encodeAtMostKZeroVarsUnsorted(bdd, vars, state, idx + 1, cnt + 1, k, hs);
    state[idx] = 1;
    encodeAtMostKZeroVarsUnsorted(bdd, vars, state, idx + 1, cnt, k, hs);
  }

  /* This method use less time and memory comparing to the unsorted method */
  public static int encodeAtMostKZeroVarsSorted(BDD bdd, int[] vars, int k) {
    return inc(vars)
        ? bdd.ref(encodeAtMostKZeroVarsSortedInc(bdd, vars, 0, k))
        : bdd.ref(encodeAtMostKZeroVarsSortedDec(bdd, vars, vars.length - 1, k));
  }

  private static int encodeAtMostKZeroVarsSortedInc(BDD bdd, int[] vars, int idx, int k) {
    // terminal cases
    if (k < 0) {
      return 0;
    }
    if (idx >= vars.length) {
      return 1;
    }
    int low = encodeAtMostKZeroVarsSortedInc(bdd, vars, idx + 1, k - 1);
    int high = encodeAtMostKZeroVarsSortedInc(bdd, vars, idx + 1, k);
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  private static int encodeAtMostKZeroVarsSortedDec(BDD bdd, int[] vars, int idx, int k) {
    // terminal cases
    if (k < 0) {
      return 0;
    }
    if (idx < 0) {
      return 1;
    }
    int low = encodeAtMostKZeroVarsSortedDec(bdd, vars, idx - 1, k - 1);
    int high = encodeAtMostKZeroVarsSortedDec(bdd, vars, idx - 1, k);
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  public static int encodeKZeroVarsSorted(BDD bdd, int[] vars, int k) {
    return inc(vars)
        ? bdd.ref(encodeKZeroVarsSortedInc(bdd, vars, 0, k))
        : bdd.ref(encodeKZeroVarsSortedDec(bdd, vars, vars.length - 1, k));
  }

  private static int encodeKZeroVarsSortedInc(BDD bdd, int[] vars, int idx, int k) {
    if (idx >= vars.length) {
      return k == 0 ? 1 : 0;
    }
    int low = 0, high = encodeKZeroVarsSortedInc(bdd, vars, idx + 1, k);
    if (k > 0) {
      low = encodeKZeroVarsSortedInc(bdd, vars, idx + 1, k - 1);
    }
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  private static int encodeKZeroVarsSortedDec(BDD bdd, int[] vars, int idx, int k) {
    if (idx < 0) {
      return k == 0 ? 1 : 0;
    }
    int low = 0, high = encodeKZeroVarsSortedDec(bdd, vars, idx - 1, k);
    if (k > 0) {
      low = encodeKZeroVarsSortedDec(bdd, vars, idx - 1, k - 1);
    }
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  /** Encode exactly k ones. */
  public static int encodeKOneVarsSorted(BDD bdd, int[] vars, int k) {
    return vars.length >= 2 && vars[1] > vars[0]
        ? bdd.ref(encodeKOneVarsSortedInc(bdd, vars, 0, k))
        : bdd.ref(encodeKOneVarsSortedDec(bdd, vars, vars.length - 1, k));
  }

  private static int encodeKOneVarsSortedInc(BDD bdd, int[] vars, int idx, int k) {
    if (idx >= vars.length) {
      return k == 0 ? 1 : 0;
    }
    int low = encodeKOneVarsSortedInc(bdd, vars, idx + 1, k), high = 0;
    if (k > 0) {
      high = encodeKOneVarsSortedInc(bdd, vars, idx + 1, k - 1);
    }
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  private static int encodeKOneVarsSortedDec(BDD bdd, int[] vars, int idx, int k) {
    if (idx < 0) {
      return k == 0 ? 1 : 0;
    }
    int low = encodeKOneVarsSortedDec(bdd, vars, idx - 1, k), high = 0;
    if (k > 0) {
      high = encodeKOneVarsSortedDec(bdd, vars, idx - 1, k - 1);
    }
    return bdd.mk(bdd.getVar(vars[idx]), low, high);
  }

  /** Count number of branches leading to one. */
  public static int branchCount(BDD bdd, int predicate) {
    if (predicate <= 1) return predicate;
    else return branchCount(bdd, bdd.getLow(predicate)) + branchCount(bdd, bdd.getHigh(predicate));
  }

  public static byte[] oneSat(BDD bdd, int predicate) {
    byte[] bin = new byte[bdd.numberOfVariables()];
    Arrays.fill(bin, (byte) -1);
    boolean found = oneSatRec(bdd, predicate, bin);
    return found ? bin : null;
  }

  public static boolean oneSatRec(BDD bdd, int predicate, byte[] bin) {
    if (predicate != 0) {
      if (predicate == 1) {
        return true;
      } else {
        int var = bdd.getVar(predicate);
        int idx = bin.length - 1 - var;
        bin[idx] = 0;
        if (oneSatRec(bdd, bdd.getLow(predicate), bin)) return true;
        bin[idx] = 1;
        if (oneSatRec(bdd, bdd.getHigh(predicate), bin)) return true;
        bin[idx] = -1;
      }
    }
    return false;
  }

  /** Find all branches leading to one. */
  public static HashSet<byte[]> allSat(BDD bdd, int predicate) {
    byte[] bin = new byte[bdd.numberOfVariables()];
    Arrays.fill(bin, (byte) -1);
    HashSet<byte[]> collection = new HashSet<>();
    allSatRec(bdd, predicate, bin, collection);
    return collection;
  }

  public static void allSatRec(BDD bdd, int predicate, byte[] bin, HashSet<byte[]> collection) {
    if (predicate != 0) {
      if (predicate == 1) {
        collection.add(bin.clone());
      } else {
        int var = bdd.getVar(predicate);
        int idx = bin.length - 1 - var;
        bin[idx] = 0;
        allSatRec(bdd, bdd.getLow(predicate), bin, collection);
        bin[idx] = 1;
        allSatRec(bdd, bdd.getHigh(predicate), bin, collection);
        bin[idx] = -1;
      }
    }
  }

  public static int add(BDD bdd, int set, int toAdd) {
    return bdd.ref(bdd.and(restrict(bdd, set, toAdd), toAdd));
  }

  public static int delete(BDD bdd, int set, int toDelete) {
    return bdd.ref(bdd.and(restrict(bdd, set, toDelete), bdd.not(toDelete)));
  }

  public static int restrict(BDD bdd, int set, int toRestrict) {
    int setP = bdd.ref(bdd.restrict(set, toRestrict));
    int setN = bdd.ref(bdd.restrict(set, bdd.not(toRestrict)));
    return bdd.ref(bdd.or(setP, setN));
  }
}
