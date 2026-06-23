package atomic.bdd;

import java.util.Objects;

public class BddRepresentedImpl<T> implements BddRepresented {
  T owner;
  int bdd;

  public BddRepresentedImpl(T owner, int bdd) {
    this.owner = owner;
    this.bdd = bdd;
  }

  public T getOwner() {
    return owner;
  }

  @Override
  public int toBdd() {
    return bdd;
  }

  @Override
  public boolean equals(Object o1) {
    if (this == o1) return true;
    if (o1 == null || getClass() != o1.getClass()) return false;
    BddRepresentedImpl<?> that = (BddRepresentedImpl<?>) o1;
    return bdd == that.bdd && Objects.equals(owner, that.owner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, bdd);
  }
}
