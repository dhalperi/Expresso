package datamodel.acl;

public abstract class AclLineMatch {
  public abstract <T> T accept(GenericAclLineMatchVisitor<T> visitor);
}
