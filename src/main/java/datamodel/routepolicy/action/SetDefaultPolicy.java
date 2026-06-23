package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.route.RouteFilterEnvironment;

import java.util.Objects;

public class SetDefaultPolicy implements Action {
  private final String defaultPolicy;

  public SetDefaultPolicy(String defaultPolicy) {
    this.defaultPolicy = defaultPolicy;
  }

  public String getDefaultPolicy() {
    return defaultPolicy;
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    environment.setDefaultPolicy(defaultPolicy);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetDefaultPolicy that = (SetDefaultPolicy) o;
    return Objects.equals(defaultPolicy, that.defaultPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultPolicy);
  }
}
