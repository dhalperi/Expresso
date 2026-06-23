package datamodel.trafficpolicy.classifier;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

public class MatchAny implements Match {
  private final List<Match> disjunctions;

  public MatchAny(List<Match> disjunctions) {
    this.disjunctions =
        disjunctions instanceof ImmutableList ? disjunctions : ImmutableList.copyOf(disjunctions);
  }

  public List<Match> getDisjunctions() {
    return disjunctions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchAny matchAny = (MatchAny) o;
    return Objects.equals(disjunctions, matchAny.disjunctions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(disjunctions);
  }

  @Override
  public <T> T accept(GenericMatchVisitor<T> visitor) {
    return visitor.visitMatchAny(this);
  }
}
