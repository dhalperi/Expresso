package datamodel.trafficpolicy.classifier;

public interface Match {
    <T> T accept(GenericMatchVisitor<T> visitor);
}
