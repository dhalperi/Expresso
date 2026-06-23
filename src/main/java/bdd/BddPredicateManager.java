package bdd;

import atomic.automaton.RegexRepresented;
import datamodel.community.CommunityRegex;
import jdd.bdd.BDD;
import main.ExpressoLogger;
import util.BddUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BddPredicateManager<T extends RegexRepresented> extends PredicateManager<T> {
  final BDD bdd;

  final int[] vars;
  final int CONSTRAINT;
  final int EMPTY;
  final int UNIVERSE;

  public BddPredicateManager(Set<T> origins) {
    super(origins);

    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "using BddPredicateManager");
    bdd = new BDD(10000, 1000);

    vars = new int[origins.size()];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = bdd.createVar();
    }

    // todo init constraints among origin variables
    CONSTRAINT = initConstraints(origins);

    EMPTY = ref(bdd.not(BddUtil.orInBatch(bdd, vars)));
    UNIVERSE = ref(1);
  }

  private int initConstraints(Set<T> origins) {
    if (origins.iterator().next() instanceof CommunityRegex) {
      Set<CommunityRegex> exacts =
          origins.stream()
              .map(o -> (CommunityRegex) o)
              .filter(o -> o.getType() == CommunityRegex.Type.EXACT)
              .collect(Collectors.toSet());
      Set<CommunityRegex> regexes =
          origins.stream()
              .map(o -> (CommunityRegex) o)
              .filter(o -> o.getType() == CommunityRegex.Type.REGEX)
              .collect(Collectors.toSet());
      SortedMap<Integer, SortedSet<Integer>> map = new TreeMap<>();
      for (CommunityRegex regex : regexes) {
        int i1 = originsBiMap.get(regex);
        Pattern pattern = Pattern.compile(regex.getRegex());
        SortedSet<Integer> children =
            exacts.stream()
                .filter(exact -> pattern.matcher(exact.getRegex()).matches())
                .map(originsBiMap::get)
                .collect(Collectors.toCollection(TreeSet::new));
        map.put(i1, children);
      }
      return BddUtil.andInBatch(
          bdd,
          map.entrySet().stream()
              .map(e -> bdd.ref(bdd.biimp(e.getKey(), BddUtil.orInBatch(bdd, e.getValue()))))
              .collect(Collectors.toSet()));
    }
    return 1;
  }

  @Override
  public int encodeOrigin(T origin) {
    return ref(vars[originsBiMap.get(origin)]);
  }

  @Override
  public int encodeOrigins(Collection<T> origins) {
    Set<Integer> set = origins.stream().map(this::encodeOrigin).collect(Collectors.toSet());
    return ref(BddUtil.andInBatch(bdd, set));
  }

  @Override
  public int encodeOnlyOrigins(Collection<T> origins) {
    Set<Integer> nots = new HashSet<>(originsBiMap.values());
    origins.stream().map(originsBiMap::get).collect(Collectors.toList()).forEach(nots::remove);

    int origin = encodeOrigins(origins);
    int onlyOrigin =
        bdd.ref(
            bdd.not(
                BddUtil.orInBatch(
                    bdd, nots.stream().map(not -> vars[not]).collect(Collectors.toSet()))));
    onlyOrigin = bdd.andTo(onlyOrigin, origin);

    return ref(onlyOrigin);
  }

  @Override
  public String getString(int v) {
    int[] oneSat = new int[vars.length];
    bdd.oneSat(v, oneSat);
    Set<String> includes = new HashSet<>();
    for (int i = 0; i < oneSat.length; i++) {
      if (oneSat[i] == 1) {
        includes.add(getPredicateString(i));
      }
    }
    return String.format("[%s]", includes.stream().sorted().collect(Collectors.joining(", ")));
  }

  private String getPredicateString(int i) {
    T origin = originsBiMap.inverse().get(i);
    return origin.getRegex();
  }

  @Override
  public int intersect(int i, int j) {
    return ref(bdd.and(i, j));
  }

  @Override
  public int diff(int i, int j) {
    return ref(bdd.and(i, bdd.not(j)));
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
    return ref(BddUtil.add(bdd, i, vars[originsBiMap.get(origin)]));
  }

  @Override
  public int add(int i, Collection<T> origins) {
    return ref(
        BddUtil.andInBatch(bdd, origins.stream().map(o -> add(i, o)).collect(Collectors.toSet())));
  }

  @Override
  public int delete(int i, T origin) {
    return ref(BddUtil.delete(bdd, i, vars[originsBiMap.get(origin)]));
  }

  @Override
  public int delete(int i, Collection<T> origins) {
    return ref(
        BddUtil.andInBatch(
            bdd, origins.stream().map(o -> delete(i, o)).collect(Collectors.toSet())));
  }

  @Override
  public int contains(int i, T origin) {
    return ref(bdd.and(i, vars[originsBiMap.get(origin)]));
  }

  @Override
  public int containsOne(int i, Collection<T> origins) {
    return ref(
        BddUtil.orInBatch(
            bdd, origins.stream().map(o -> contains(i, o)).collect(Collectors.toSet())));
  }

  @Override
  public int containsAll(int i, Collection<T> origins) {
    return ref(
        BddUtil.andInBatch(
            bdd, origins.stream().map(o -> contains(i, o)).collect(Collectors.toSet())));
  }

  private int ref(int i) {
    return bdd.ref(bdd.and(i, CONSTRAINT));
  }
}
