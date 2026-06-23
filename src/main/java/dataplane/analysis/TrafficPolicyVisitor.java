package dataplane.analysis;

import com.google.common.collect.ImmutableList;
import datamodel.trafficpolicy.GenericTrafficPolicyVisitor;
import datamodel.trafficpolicy.TrafficPolicy;
import datamodel.trafficpolicy.behavior.TrafficBehavior;
import datamodel.trafficpolicy.classifier.TrafficClassifier;
import javafx.util.Pair;
import main.Controller;

import java.util.List;

public class TrafficPolicyVisitor
    implements GenericTrafficPolicyVisitor<List<Pair<Integer, String>>, Integer> {
  final int packet;

  public TrafficPolicyVisitor(int packet) {
    this.packet = packet;
  }

  @Override
  public List<Pair<Integer, String>> visitInboundTrafficPolicy(TrafficPolicy trafficPolicy) {
    ImmutableList.Builder<Pair<Integer, String>> result = ImmutableList.builder();

    int pkt = packet;
    for (Pair<TrafficClassifier, TrafficBehavior> line : trafficPolicy.getLines()) {
      TrafficClassifierVisitor classifierVisitor =
          new TrafficClassifierVisitor(Controller.bddManager.getBDD(), pkt);
      int matched = classifierVisitor.visit(line.getKey().getMatch());
      TrafficBehaviorVisitor.InboundVisitor inboundVisitor =
          new TrafficBehaviorVisitor.InboundVisitor(matched);
      result.addAll(inboundVisitor.visit(line.getValue().getAction()));
      pkt = Controller.bddManager.minus1(pkt, matched);
    }

    // apply default action
    if (pkt != 0) {
      TrafficBehaviorVisitor.InboundVisitor inboundVisitor =
          new TrafficBehaviorVisitor.InboundVisitor(pkt);
      result.addAll(inboundVisitor.visit(trafficPolicy.getDefaultAction().getAction()));
    }

    return result.build();
  }

  @Override
  public Integer visitOutboundTrafficPolicy(TrafficPolicy trafficPolicy) {
    int permitted = 0;

    int pkt = packet;
    for (Pair<TrafficClassifier, TrafficBehavior> line : trafficPolicy.getLines()) {
      TrafficClassifierVisitor classifierVisitor =
          new TrafficClassifierVisitor(Controller.bddManager.getBDD(), pkt);
      int matched = classifierVisitor.visit(line.getKey().getMatch());
      TrafficBehaviorVisitor.OutboundVisitor outboundVisitor =
          new TrafficBehaviorVisitor.OutboundVisitor(matched);
      int permit = outboundVisitor.visit(line.getValue().getAction());
      int tmp = Controller.bddManager.or(permitted, permit);
      Controller.bddManager.deref(permitted);
      permitted = tmp;
      pkt = Controller.bddManager.minus1(pkt, matched);
    }

    if (pkt != 0) {
      TrafficBehaviorVisitor.OutboundVisitor outboundVisitor =
          new TrafficBehaviorVisitor.OutboundVisitor(pkt);
      int permit = outboundVisitor.visit(trafficPolicy.getDefaultAction().getAction());
      int tmp = Controller.bddManager.or(permitted, permit);
      Controller.bddManager.deref(permitted);
      permitted = tmp;
    }

    return permitted;
  }
}
