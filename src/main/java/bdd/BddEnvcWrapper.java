package bdd;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import controlplane.process.bgp.ISP;
import javafx.util.Pair;
import jdd.bdd.BDD;
import jdd.bdd.Permutation;
import main.Configuration;
import main.ExpressoLogger;
import util.BddUtil;

import java.util.*;
import java.util.stream.Collectors;

/** Envc stands for external environment condition. */
public class BddEnvcWrapper extends BddWrapper {
  public static class IspVarWrapper {
    ISP isp;
    int cpVar;
    // prefix length -> bdd variable
    BiMap<Integer, Integer> dpVars;
    int cpDp = -1;

    public IspVarWrapper(ISP isp) {
      this.isp = isp;
      this.dpVars = HashBiMap.create();
    }

    public int getCpDpRelation(BDD bdd) {
      return cpDp != -1
          ? cpDp
          : (cpDp = bdd.ref(bdd.biimp(cpVar, BddUtil.orInBatch(bdd, dpVars.values()))));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IspVarWrapper ispVarWrapper = (IspVarWrapper) o;
      return Objects.equals(isp, ispVarWrapper.isp);
    }

    @Override
    public int hashCode() {
      return Objects.hash(isp);
    }
  }

  HashMap<ISP, IspVarWrapper> ispVars;
  HashMap<Integer, ISP> varNo;

  public BddEnvcWrapper(BddManager manager) {
    super(manager);

    ispVars = new HashMap<>();
    varNo = new HashMap<>();
  }

  public int addIspVar(ISP isp) {
    if (!ispVars.containsKey(isp)) {
      IspVarWrapper wrapper = new IspVarWrapper(isp);
      if (Configuration.ISP_VAR) {
        wrapper.cpVar = addVar(isp);
      } else {
        wrapper.cpVar = 1;
      }
      //            for (int i = 32; i >= 0; i--) wrapper.dpVars.put(i, addVar(isp));
      //            for (int i = 0; i <= 32; i++) wrapper.dpVars.put(i, addVar(isp));
      //            wrapper.dpVars.put(0, wrapper.cpVar);
      ispVars.put(isp, wrapper);
    }
    return ispVars.get(isp).cpVar;
  }

  private int addVar(ISP isp) {
    int var = bdd.createVar();
    int no = bdd.getVar(var);
    varNo.put(no, isp);
    return var;
  }

  public int numIspVar() {
    return ispVars.size();
  }

  public HashMap<ISP, IspVarWrapper> getIspVars() {
    return ispVars;
  }

  /**
   * @param i the sequential number of the {@link ISP}
   */
  public ISP getIsp(int i) {
    // pay attention to how we get var!
    int var = (ispVars.size() - 1 - i) + BddPrefixWrapper.prefixBits;
    return varNo.get(var);
  }

  public String getEnvc(byte[] envcBv) {
    List<String> list = new LinkedList<>();
    for (int i = 0; i < envcBv.length; i++) {
      if (envcBv[i] != -1) {
        list.add((envcBv[i] == 0 ? "!" : "") + getIsp(i).getName());
      }
    }
    return String.join(" & ", list);
  }

  private List<IspVarWrapper> varset(int predicate) {
    HashSet<IspVarWrapper> hs = new HashSet<>();
    HashSet<Integer> seen = new HashSet<>();
    Queue<Integer> queue = new LinkedList<>();
    queue.add(predicate);
    while (!queue.isEmpty()) {
      int top = queue.poll();
      seen.add(top);
      int var = bdd.getVar(top);
      if (var > BddPrefixWrapper.prefixBits) {
        hs.add(ispVars.get(varNo.get(var)));
      }
      int low, high;
      if ((low = bdd.getLow(top)) > 1 && !seen.contains(low)) queue.add(low);
      if ((high = bdd.getHigh(top)) > 1 && !seen.contains(high)) queue.add(high);
    }
    // sort by the cpVar, from large to small
    return hs.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingInt(ispVar -> -ispVar.cpVar))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private final Permutation[] perms = new Permutation[33];

  private Permutation getPermutation(int length) {
    if (perms[length] == null) {
      int[] from = new int[ispVars.size()];
      int[] to = new int[ispVars.size()];
      int i = 0;
      for (IspVarWrapper wrapper : ispVars.values()) {
        from[i] = wrapper.cpVar;
        to[i] = wrapper.dpVars.computeIfAbsent(length, k -> addVar(wrapper.isp));
        i++;
      }
      perms[length] = bdd.createPermutation(from, to);
    }
    return perms[length];
  }

  private final HashSet<Integer> emptyPerms = new HashSet<>();
  private final Table<Integer, Integer, Permutation> permsTable = HashBasedTable.create();

  private Permutation getPermutation(int predicate, int length) {
    if (emptyPerms.contains(predicate)) {
      return null;
    } else {
      if (!permsTable.contains(predicate, length)) {
        List<IspVarWrapper> varset = varset(predicate);
        if (varset.isEmpty()) {
          emptyPerms.add(predicate);
          return null;
        } else {
          int[] from = new int[varset.size()];
          int[] to = new int[varset.size()];
          int i = 0;
          for (IspVarWrapper wrapper : varset) {
            from[i] = wrapper.cpVar;
            to[i] = wrapper.dpVars.computeIfAbsent(length, k -> addVar(wrapper.isp));
            i++;
          }
          Permutation perm = bdd.createPermutation(from, to);
          permsTable.put(predicate, length, perm);
        }
      }
      return permsTable.get(predicate, length);
    }
  }

  private final HashMap<Pair<Integer, Integer>, List<Permutation>> permsMap = new HashMap<>();

  private List<Permutation> getPermutations(int predicate, int length) {
    Pair<Integer, Integer> pair = new Pair<>(predicate, length);
    if (!permsMap.containsKey(pair)) {
      List<IspVarWrapper> varset = varset(predicate);
      if (varset.isEmpty()) {
        permsMap.put(pair, Collections.emptyList());
      } else {
        List<Permutation> list = new LinkedList<>();
        int i = 0;
        while (i < varset.size()) {
          int s = Integer.min(Configuration.ENVC_PERM_SIZE, varset.size() - i);
          int[] from = new int[s];
          int[] to = new int[s];
          int j = 0;
          for (IspVarWrapper wrapper : varset.subList(i, i + s)) {
            from[j] = wrapper.cpVar;
            to[j] = wrapper.dpVars.computeIfAbsent(length, k -> addVar(wrapper.isp));
            j++;
          }
          Permutation perm = bdd.createPermutation(from, to);
          list.add(perm);
          i += Configuration.ENVC_PERM_SIZE;
        }
        permsMap.put(pair, list);
      }
    }
    return permsMap.get(pair);
  }

  public int transformCpEnvc2DpEnvc(int predicate, int length) {
    //        Permutation perm = getPermutation(predicate, length);
    //        return perm == null ? predicate : bdd.ref(bdd.replace(predicate, perm));

    List<Permutation> perms = getPermutations(predicate, length);
    int tmp = predicate;
    for (Permutation perm : perms) {
      tmp = bdd.ref(bdd.replace(tmp, perm));
    }
    return tmp;

    //        Permutation perm = getPermutation(length);
    //        return bdd.ref(bdd.replace(predicate, perm));
  }

  public int transformCpEnvc2DpEnvcMoreSpecific(int predicate, int length) {
    int tmp, intersect;
    for (int len = 32; len > length; len--) {
      tmp = manager.bddPrefixWrapper.encodeLength(len);
      intersect = manager.and(predicate, tmp);
      if (intersect == 0) continue;
      intersect = manager.bddPrefixWrapper.eraseLength(predicate);
      predicate = transformCpEnvc2DpEnvc(intersect, len);
    }
    return predicate;
  }

  HashMap<Integer, HashSet<byte[]>> bvMemo = new HashMap<>();
  int bvMemoHit = 0;

  public HashSet<byte[]> getEnvcBitVectors(int predicate) {
    if (!bvMemo.containsKey(predicate)) {
      byte[] bv = new byte[ispVars.size()];
      bvMemo.put(predicate, getEnvcBitVectors(predicate, bv));
    } else {
      bvMemoHit++;
    }
    return bvMemo.get(predicate);
  }

  public HashSet<byte[]> getEnvcBitVectors(int predicate, byte[] bv) {
    if (predicate != 0) {
      if (predicate == 1) {
        HashSet<byte[]> result = new HashSet<>();
        result.add(Arrays.copyOf(bv, bv.length));
        return result;
      } else {
        int var = bdd.getVar(predicate);
        if (var <= BddPrefixWrapper.prefixBits) {
          HashSet<byte[]> low = getEnvcBitVectors(bdd.getLow(predicate), bv);
          HashSet<byte[]> high = getEnvcBitVectors(bdd.getHigh(predicate), bv);
          low.addAll(high);
          return low;
        } else {
          int idx = bv.length - 1 - var - BddPrefixWrapper.prefixBits;
          if (bdd.getLow(predicate) == 1) {
            HashSet<byte[]> result = new HashSet<>();
            byte[] low = Arrays.copyOf(bv, bv.length);
            low[idx] = 0;
            result.add(low);
            result.addAll(getEnvcBitVectors(bdd.getHigh(predicate), bv));
            return result;
          } else if (bdd.getHigh(predicate) == 1) {
            HashSet<byte[]> result = new HashSet<>();
            byte[] high = Arrays.copyOf(bv, bv.length);
            high[idx] = 1;
            result.add(high);
            result.addAll(getEnvcBitVectors(bdd.getLow(predicate), bv));
            return result;
          } else {
            bv[idx] = 0;
            HashSet<byte[]> result = new HashSet<>(getEnvcBitVectors(bdd.getLow(predicate), bv));
            bv[idx] = 1;
            result.addAll(getEnvcBitVectors(bdd.getHigh(predicate), bv));
            bv[idx] = -1;
            return result;
          }
        }
      }
    }
    return new HashSet<>();
  }

  public void logIspVars() {
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "isp variables: " + varNo.size());
  }
}
