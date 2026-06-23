package datamodel.acl;

import java.util.LinkedList;
import java.util.List;

public class AclLineMatchOr extends AclLineMatch {
  List<AclLineMatch> disjunctions;

  public AclLineMatchOr() {
    disjunctions = new LinkedList<>();
  }

  public AclLineMatchOr(List<AclLineMatch> disjunctions) {
    this.disjunctions = disjunctions;
  }

  public List<AclLineMatch> getDisjunctions() {
    return disjunctions;
  }

  @Override
  public <T> T accept(GenericAclLineMatchVisitor<T> visitor) {
    return visitor.visitAclLineMatchOr(this);
  }
}
