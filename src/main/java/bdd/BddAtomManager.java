package bdd;

import atomic.AtomicPredicates;
import com.google.common.annotations.VisibleForTesting;
import jdd.bdd.BDD;
import main.ExpressoLogger;
import util.BddUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BddAtomManager<T, A> extends AtomManager<T, A> {
  final BDD bdd;

  final int[] vars;
  final int EMPTY;
  final int UNIVERSE;

  public BddAtomManager(AtomicPredicates<T, A> atomicPredicates) {
    super(atomicPredicates);
    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "using BddAtomManager");
    bdd = new BDD(10000, 1000);

    vars = new int[atomicPredicates.getNumAtoms()];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = bdd.createVar();
    }

    EMPTY = bdd.ref(bdd.not(BddUtil.orInBatch(bdd, vars)));
    UNIVERSE = 1;
  }

  @Override
  public int encodeOrigin(T origin) {
    Set<Integer> aps = atomicPredicates.getOriginToAtoms().get(origin);
    Set<Integer> apVars = aps.stream().map(ap -> vars[ap]).collect(Collectors.toSet());
    return BddUtil.orInBatch(bdd, apVars);
  }

  @Override
  public int encodeOrigins(Collection<T> origins) {
    Set<Integer> set = origins.stream().map(this::encodeOrigin).collect(Collectors.toSet());
    return BddUtil.andInBatch(bdd, set);
  }

  @Override
  public int encodeOnlyOrigins(Collection<T> origins) {
    Set<Integer> nots = new HashSet<>(atomicPredicates.getAtoms().keySet());
    origins.forEach(o -> nots.removeAll(atomicPredicates.getOriginToAtoms().get(o)));

    int origin = encodeOrigins(origins);
    int onlyOrigin =
        bdd.ref(
            bdd.not(
                BddUtil.orInBatch(
                    bdd, nots.stream().map(ap -> vars[ap]).collect(Collectors.toSet()))));
    onlyOrigin = bdd.andTo(onlyOrigin, origin);

    return onlyOrigin;
  }

  @Override
  public String getString(int v) {
    int[] oneSat = new int[vars.length];
    bdd.oneSat(v, oneSat);
    Set<String> includes = new HashSet<>();
    for (int i = 0; i < oneSat.length; i++) {
      if (oneSat[i] == 1) {
        includes.add(getAtomString(i));
      }
    }
    return String.format("[%s]", includes.stream().sorted().collect(Collectors.joining(", ")));
  }

  @Override
  public int intersect(int i, int j) {
    return bdd.ref(bdd.and(i, j));
  }

  @Override
  public int diff(int i, int j) {
    return bdd.ref(bdd.and(i, bdd.not(j)));
  }

  @Override
  public int empty() {
    return EMPTY;
  }

  @Override
  public int universe() {
    return UNIVERSE;
  }

  @Override
  public int set(Collection<T> origins) {
    return encodeOnlyOrigins(origins);
  }

  @Override
  public int add(int i, T origin) {
    return BddUtil.orInBatch(
        bdd,
        atomicPredicates.getOriginToAtoms().get(origin).stream()
            .map(ap -> add(i, ap))
            .collect(Collectors.toSet()));
  }

  @Override
  public int add(int i, Collection<T> origins) {
    return BddUtil.andInBatch(
        bdd, origins.stream().map(o -> add(i, o)).collect(Collectors.toSet()));
  }

  @Override
  public int delete(int i, T origin) {
    // todo check correctness
    return BddUtil.andInBatch(
        bdd,
        atomicPredicates.getOriginToAtoms().get(origin).stream()
            .map(ap -> delete(i, ap))
            .collect(Collectors.toSet()));
  }

  @Override
  public int delete(int i, Collection<T> origins) {
    // todo check correctness
    return BddUtil.andInBatch(
        bdd, origins.stream().map(o -> delete(i, o)).collect(Collectors.toSet()));
  }

  @Override
  public int contains(int i, T origin) {
    int o = encodeOrigin(origin);
    return bdd.ref(bdd.and(i, o));
  }

  @Override
  public int containsOne(int i, Collection<T> origins) {
    return BddUtil.orInBatch(
        bdd, origins.stream().map(o -> contains(i, o)).collect(Collectors.toSet()));
  }

  @Override
  public int containsAll(int i, Collection<T> origins) {
    return BddUtil.andInBatch(
        bdd, origins.stream().map(o -> contains(i, o)).collect(Collectors.toSet()));
  }

  public static boolean useRestrict = true;

  @VisibleForTesting
  int add(int set, int var) {
    if (useRestrict) {
      return bdd.and(restrict(set, vars[var]), vars[var]);
    } else {
      if (set == 0) return 0;
      if (set == 1) return bdd.ref(bdd.mk(var, 0, 1));
      return addRec(set, var);
    }
  }

  int addRec(int set, int var) {
    int curVar = bdd.getVar(set);
    if (curVar < var) {
      int low = addRec(bdd.getLow(set), var);
      int high = addRec(bdd.getHigh(set), var);
      return bdd.ref(bdd.mk(curVar, low, high));
    } else if (curVar == var) {
      // set[var] = 0/1
      int low = bdd.getLow(set);
      low = low == 0 ? 1 : low;
      int high = bdd.getHigh(set);
      return bdd.ref(bdd.mk(var, 0, bdd.or(low, high)));
    } else {
      // set[var] = *
      return bdd.ref(bdd.mk(var, 0, set));
    }
  }

  @VisibleForTesting
  int delete(int set, int var) {
    if (useRestrict) {
      return bdd.ref(bdd.and(restrict(set, vars[var]), bdd.not(vars[var])));
    } else {
      if (set == 0) return 0;
      if (set == 1) return bdd.ref(bdd.mk(var, 1, 0));
      return deleteRec(set, var);
    }
  }

  int deleteRec(int set, int var) {
    int curVar = bdd.getVar(set);
    if (curVar < var) {
      int low = deleteRec(bdd.getLow(set), var);
      int high = deleteRec(bdd.getHigh(set), var);
      return bdd.ref(bdd.mk(curVar, low, high));
    } else if (curVar == var) {
      // set[var] = 0/1
      int low = bdd.getLow(set);
      int high = bdd.getHigh(set);
      high = high == 0 ? 1 : high;
      return bdd.ref(bdd.mk(var, bdd.or(low, high), 0));
    } else {
      // set[var] = *
      return bdd.ref(bdd.mk(var, set, 0));
    }
  }

  int restrict(int set, int var) {
    int setP = bdd.ref(bdd.restrict(set, var));
    int setN = bdd.ref(bdd.restrict(set, bdd.not(var)));
    return bdd.ref(bdd.or(setP, setN));
  }
}
