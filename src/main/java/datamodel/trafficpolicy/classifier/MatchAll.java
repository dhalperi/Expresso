package datamodel.trafficpolicy.classifier;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

public class MatchAll implements Match {
  private final List<Match> conjunctions;

  public MatchAll(List<Match> conjunctions) {
    this.conjunctions =
        conjunctions instanceof ImmutableList ? conjunctions : ImmutableList.copyOf(conjunctions);
  }

  public List<Match> getConjunctions() {
    return conjunctions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchAll matchAll = (MatchAll) o;
    return Objects.equals(conjunctions, matchAll.conjunctions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conjunctions);
  }

  @Override
  public <T> T accept(GenericMatchVisitor<T> visitor) {
    return visitor.visitMatchAll(this);
  }
}
