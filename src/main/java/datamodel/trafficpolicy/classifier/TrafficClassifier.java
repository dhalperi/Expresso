package datamodel.trafficpolicy.classifier;

public class TrafficClassifier {
  public static final TrafficClassifier ALL = new TrafficClassifier("all", MatchAlways.TRUE);
  public static final TrafficClassifier NONE = new TrafficClassifier("none", MatchAlways.FALSE);

  private final String name;
  private final Match match;

  public TrafficClassifier(String name, Match match) {
    this.name = name;
    this.match = match;
  }

  public String getName() {
    return name;
  }

  public Match getMatch() {
    return match;
  }
}
