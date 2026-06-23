package atomic;

import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AtomicPredicates<T, A> {
  @Nonnull protected final Set<T> _origins;

  // the number of atomic predicates
  protected int _numAtoms;

  // maps each regex to its set of atomic predicates, which are integers in the range
  // 0 ... (_numAtomicPredicates - 1)
  @Nonnull protected Map<T, Set<Integer>> _originToAtoms;

  // maps each atomic predicate number to its semantic representation, which is a finite-state
  // automaton
  @Nonnull protected Map<Integer, Set<T>> _atomToOrigins;

  @Nonnull protected Map<Integer, A> _atoms;

  public AtomicPredicates(@Nonnull Set<T> origins) {
    _origins = origins;
    initAtoms();
  }

  protected abstract void initAtoms();

  /**
   * 1. Assign a unique integer to each atom (i.e., {@link AtomicPredicates#_atoms}). <br>
   * 2. Create a mapping from each integer to its corresponding atom (i.e., {@link
   * AtomicPredicates#_numAtoms}). <br>
   * 3. Create a mapping from each integer to its corresponding origins (i.e., {@link
   * AtomicPredicates#_atomToOrigins}). <br>
   * 4. Create a mapping from each origin to its corresponding set of integers (i.e., {@link
   * AtomicPredicates#_originToAtoms}).
   *
   * @param mmap a {@link Multimap} mapping from atoms to origins
   */
  protected void initMaps(Multimap<A, T> mmap) {
    _originToAtoms = new HashMap<>();
    _atomToOrigins = new HashMap<>();
    _atoms = new HashMap<>();
    int[] i = {0};
    for (A a : mmap.keySet()) {
      _atoms.put(i[0], a);
      _atomToOrigins.put(i[0], new HashSet<>(mmap.get(a)));
      mmap.get(a).forEach(t -> _originToAtoms.computeIfAbsent(t, x -> new HashSet<>()).add(i[0]));
      i[0]++;
    }
    _numAtoms = i[0];
  }

  public int getNumAtoms() {
    return _numAtoms;
  }

  @Nonnull
  public Map<T, Set<Integer>> getOriginToAtoms() {
    return _originToAtoms;
  }

  @Nonnull
  public Map<Integer, Set<T>> getAtomToOrigins() {
    return _atomToOrigins;
  }

  @Nonnull
  public Map<Integer, A> getAtoms() {
    return _atoms;
  }
}
