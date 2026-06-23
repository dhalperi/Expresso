package datamodel.trafficpolicy.classifier;

import java.util.Objects;

public class MatchAlways implements Match {
  public static final MatchAlways TRUE = new MatchAlways(true);
  public static final MatchAlways FALSE = new MatchAlways(false);

  private final boolean permit;

  private MatchAlways(boolean permit) {
    this.permit = permit;
  }

  public boolean isPermit() {
    return permit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchAlways that = (MatchAlways) o;
    return permit == that.permit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(permit);
  }

  @Override
  public <T> T accept(GenericMatchVisitor<T> visitor) {
    return visitor.visitMatchAlways(this);
  }
}
