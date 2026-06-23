package datamodel.trafficpolicy.classifier;

public interface GenericMatchVisitor<T> {
  default T visit(Match match) {
    return match.accept(this);
  }

  T visitMatchAcl(MatchAcl matchAcl);

  T visitMatchAll(MatchAll matchAll);

  T visitMatchAlways(MatchAlways matchAlways);

  T visitMatchAny(MatchAny matchAny);
}
