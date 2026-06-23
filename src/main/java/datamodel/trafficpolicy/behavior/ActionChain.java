package datamodel.trafficpolicy.behavior;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ActionChain implements Action {
  private final List<Action> actions;

  public ActionChain(List<Action> actions) {
    this.actions = actions instanceof ImmutableList ? actions : ImmutableList.copyOf(actions);
  }

  public List<Action> getActions() {
    return actions;
  }

  @Override
  public <T> T accept(GenericActionVisitor<T> visitor) {
    return visitor.visitActionChain(this);
  }
}
