package datamodel.trafficpolicy.behavior;

public class Permit implements Action {
  public static final Permit INSTANCE = new Permit();

  private Permit() {}

  @Override
  public <T> T accept(GenericActionVisitor<T> visitor) {
    return visitor.visitPermit(this);
  }
}
