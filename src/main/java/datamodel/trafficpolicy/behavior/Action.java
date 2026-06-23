package datamodel.trafficpolicy.behavior;

public interface Action {
  <T> T accept(GenericActionVisitor<T> visitor);
}
