package datamodel.trafficpolicy.behavior;

import datamodel.ipv4.Ip;
import javafx.util.Pair;

import java.util.List;
import java.util.Objects;

public class Redirect implements Action {
  private final List<Pair<Ip, String>> nextHops;

  public Redirect(List<Pair<Ip, String>> nextHops) {
    this.nextHops = nextHops;
  }

  public List<Pair<Ip, String>> getNextHops() {
    return nextHops;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Redirect redirect = (Redirect) o;
    return Objects.equals(nextHops, redirect.nextHops);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nextHops);
  }

  @Override
  public <T> T accept(GenericActionVisitor<T> visitor) {
    return visitor.visitRedirect(this);
  }
}
