package datamodel.trafficpolicy.classifier;

import datamodel.acl.Acl;

import java.util.Objects;

public class MatchAcl implements Match {
  private final Acl acl;

  public MatchAcl(Acl acl) {
    this.acl = acl;
  }

  public Acl getAcl() {
    return acl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatchAcl matchAcl = (MatchAcl) o;
    return Objects.equals(acl, matchAcl.acl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(acl);
  }

  @Override
  public <T> T accept(GenericMatchVisitor<T> visitor) {
    return visitor.visitMatchAcl(this);
  }
}
