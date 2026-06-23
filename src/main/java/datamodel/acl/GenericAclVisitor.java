package datamodel.acl;

public interface GenericAclVisitor<T> {
  T visitAcl(Acl acl);
}
