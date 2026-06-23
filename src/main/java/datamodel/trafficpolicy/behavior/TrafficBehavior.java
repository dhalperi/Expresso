package datamodel.trafficpolicy.behavior;

public class TrafficBehavior {
  public static final TrafficBehavior PERMIT = new TrafficBehavior("permit", Permit.INSTANCE);
  public static final TrafficBehavior DENY = new TrafficBehavior("deny", Deny.INSTANCE);

  private final String name;
  private final Action action;

  public TrafficBehavior(String name, Action action) {
    this.name = name;
    this.action = action;
  }

  public String getName() {
    return name;
  }

  public Action getAction() {
    return action;
  }
}
