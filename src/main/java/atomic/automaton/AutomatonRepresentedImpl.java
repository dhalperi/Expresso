package atomic.automaton;

import dk.brics.automaton.Automaton;

import java.util.Objects;

public class AutomatonRepresentedImpl<T> implements AutomatonRepresented {
  T owner;
  Automaton automaton;

  public AutomatonRepresentedImpl(T owner, Automaton automaton) {
    this.owner = owner;
    this.automaton = automaton;
  }

  public T getOwner() {
    return owner;
  }

  @Override
  public Automaton toAutomaton() {
    return automaton;
  }

  @Override
  public String toString() {
    return automaton.getShortestExample(true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AutomatonRepresentedImpl<?> that = (AutomatonRepresentedImpl<?>) o;
    return Objects.equals(owner, that.owner) && Objects.equals(automaton, that.automaton);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, automaton);
  }
}
