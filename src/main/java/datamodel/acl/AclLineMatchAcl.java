package datamodel.acl;

public class AclLineMatchAcl extends AclLineMatch {
  String referenceName;
  Acl referenceAcl;

  public AclLineMatchAcl(String referenceName) {
    this.referenceName = referenceName;
  }

  public String getReferenceName() {
    return referenceName;
  }

  public void setReferenceAcl(Acl referenceAcl) {
    this.referenceAcl = referenceAcl;
  }

  public Acl getReferenceAcl() {
    return referenceAcl;
  }

  @Override
  public <T> T accept(GenericAclLineMatchVisitor<T> visitor) {
    return visitor.visitAclLineMatchAcl(this);
  }
}
