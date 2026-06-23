package datamodel.trafficpolicy.behavior;

public interface GenericActionVisitor<T> {
  default T visit(Action action) {
    return action.accept(this);
  }

  T visitActionChain(ActionChain actionChain);

  T visitDeny(Deny deny);

  T visitPermit(Permit permit);

  T visitRedirect(Redirect redirect);
}
