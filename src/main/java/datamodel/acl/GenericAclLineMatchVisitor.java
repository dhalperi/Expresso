package datamodel.acl;

public interface GenericAclLineMatchVisitor<T> {
  default T visit(AclLineMatch aclLineMatch) {
    return aclLineMatch.accept(this);
  }

  T visitAclLineMatchAcl(AclLineMatchAcl aclLineMatchAcl);

  T visitAclLineMatchAnd(AclLineMatchAnd aclLineMatchAnd);

  T visitAclLineMatchOr(AclLineMatchOr aclLineMatchOr);

  T visitAclLineMatchPacketHeader(AclLineMatchPacketHeader aclLineMatchPacketHeader);
}
