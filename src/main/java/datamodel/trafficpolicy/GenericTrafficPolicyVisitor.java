package datamodel.trafficpolicy;

public interface GenericTrafficPolicyVisitor<T1, T2> {
  T1 visitInboundTrafficPolicy(TrafficPolicy trafficPolicy);

  T2 visitOutboundTrafficPolicy(TrafficPolicy trafficPolicy);
}
