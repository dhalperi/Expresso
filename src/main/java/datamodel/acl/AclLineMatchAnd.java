package datamodel.acl;

import java.util.LinkedList;
import java.util.List;

public class AclLineMatchAnd extends AclLineMatch {
  List<AclLineMatch> conjunctions;

  public AclLineMatchAnd() {
    conjunctions = new LinkedList<>();
  }

  public AclLineMatchAnd(List<AclLineMatch> conjunctions) {
    this.conjunctions = conjunctions;
  }

  public List<AclLineMatch> getConjunctions() {
    return conjunctions;
  }

  @Override
  public <T> T accept(GenericAclLineMatchVisitor<T> visitor) {
    return visitor.visitAclLineMatchAnd(this);
  }
}
