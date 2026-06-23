package datamodel.trafficpolicy.behavior;

public class Deny implements Action {
  public static final Deny INSTANCE = new Deny();

  private Deny() {}

  @Override
  public <T> T accept(GenericActionVisitor<T> visitor) {
    return visitor.visitDeny(this);
  }
}
