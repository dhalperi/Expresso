package bdd;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.util.Collection;
import java.util.Set;

public abstract class PredicateManager<T> {
  final BiMap<T, Integer> originsBiMap;

  public PredicateManager(Set<T> origins) {
    ImmutableBiMap.Builder<T, Integer> builder = ImmutableBiMap.builder();
    int i = 0;
    for (T origin : origins) {
      builder.put(origin, i);
      i++;
    }
    originsBiMap = builder.build();
  }

  /**
   * Since an origin consists of some atoms, the existence of the origin equals the existence of at
   * least one of the atoms.
   */
  public abstract int encodeOrigin(T origin);

  /** The existence of all origins. There must be at least one atom for each origin. */
  public abstract int encodeOrigins(Collection<T> origins);

  public abstract int encodeOnlyOrigins(Collection<T> origins);

  public abstract String getString(int v);

  /** Intersection between two sets of sets */
  public abstract int intersect(int i, int j);

  /** Difference between two sets of sets */
  public abstract int diff(int i, int j);

  /** A set containing an empty set. I.e., {{}} */
  public abstract int empty();

  /**
   * A set containing all subsets of atoms. E.g., for atoms = {v1, v2}, it returns {{}, {v1}, {v2},
   * {v1, v2}}.
   */
  public abstract int universe();

  public abstract int set(Collection<T> origins);

  public abstract int add(int i, T origin);

  public abstract int add(int i, Collection<T> origins);

  public abstract int delete(int i, T origin);

  public abstract int delete(int i, Collection<T> origins);

  public abstract int contains(int i, T origin);

  public abstract int containsOne(int i, Collection<T> origins);

  public abstract int containsAll(int i, Collection<T> origins);
}
