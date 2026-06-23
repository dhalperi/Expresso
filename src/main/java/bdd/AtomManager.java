package bdd;

import atomic.AtomicPredicates;
import atomic.automaton.AutomatonAtomicPredicates;
import datamodel.community.CommunityRegex;

import java.util.Optional;
import java.util.Set;

public abstract class AtomManager<T, A> extends PredicateManager<T> {
  final AtomicPredicates<T, A> atomicPredicates;

  public AtomManager(AtomicPredicates<T, A> atomicPredicates) {
    super(atomicPredicates.getOriginToAtoms().keySet());
    this.atomicPredicates = atomicPredicates;
  }

  public String getAtomString(int ap) {
    if (atomicPredicates instanceof AutomatonAtomicPredicates) {
      AutomatonAtomicPredicates<?> automatonAtomicPredicates =
          (AutomatonAtomicPredicates<?>) atomicPredicates;
      Set<?> origins = automatonAtomicPredicates.getAtomToOrigins().get(ap);
      Object origin = origins.iterator().next();
      if (origin instanceof CommunityRegex) {
        Optional<CommunityRegex> optional =
            origins.stream()
                .map(o -> (CommunityRegex) o)
                .filter(o -> o.getType() == CommunityRegex.Type.EXACT)
                .findAny();
        if (optional.isPresent()) return optional.get().getLiteralValue().toString();
      }
      return ((AutomatonAtomicPredicates<?>) atomicPredicates)
          .getAtoms()
          .get(ap)
          .getShortestExample(true);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
