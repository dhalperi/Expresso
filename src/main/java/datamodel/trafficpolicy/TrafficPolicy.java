package datamodel.trafficpolicy;

import com.google.common.collect.ImmutableList;
import datamodel.trafficpolicy.behavior.TrafficBehavior;
import datamodel.trafficpolicy.classifier.TrafficClassifier;
import javafx.util.Pair;

import java.util.List;
import java.util.Objects;

public class TrafficPolicy {
  public static final TrafficPolicy PERMIT_ALL =
      new TrafficPolicy(
          "permitAll", ImmutableList.of(new Pair<>(TrafficClassifier.ALL, TrafficBehavior.PERMIT)));

  private final String name;
  private final List<Pair<TrafficClassifier, TrafficBehavior>> lines;
  private final TrafficBehavior defaultAction;
  private boolean used;

  public TrafficPolicy(String name, List<Pair<TrafficClassifier, TrafficBehavior>> lines) {
    this(name, lines, TrafficBehavior.PERMIT);
  }

  public TrafficPolicy(
      String name,
      List<Pair<TrafficClassifier, TrafficBehavior>> lines,
      TrafficBehavior defaultAction) {
    this.name = name;
    this.lines = lines;
    this.defaultAction = defaultAction;
  }

  public String getName() {
    return name;
  }

  public List<Pair<TrafficClassifier, TrafficBehavior>> getLines() {
    return lines;
  }

  public TrafficBehavior getDefaultAction() {
    return defaultAction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TrafficPolicy that = (TrafficPolicy) o;
    return Objects.equals(name, that.name) && Objects.equals(lines, that.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, lines);
  }

  public void setUsed(boolean used) {
    this.used = used;
  }

  public boolean isUsed() {
    return used;
  }
}
