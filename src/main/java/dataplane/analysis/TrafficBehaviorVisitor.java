package dataplane.analysis;

import com.google.common.collect.ImmutableList;
import datamodel.trafficpolicy.behavior.*;
import javafx.util.Pair;
import main.Controller;
import util.BddUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TrafficBehaviorVisitor {
  private TrafficBehaviorVisitor() {}

  public static class InboundVisitor implements GenericActionVisitor<List<Pair<Integer, String>>> {
    final int packet;

    public InboundVisitor(int packet) {
      this.packet = packet;
    }

    @Override
    public List<Pair<Integer, String>> visitActionChain(ActionChain actionChain) {
      return actionChain.getActions().stream()
          .flatMap(action -> action.accept(this).stream())
          .collect(Collectors.toList());
    }

    @Override
    public List<Pair<Integer, String>> visitDeny(Deny deny) {
      return Collections.emptyList();
    }

    @Override
    public List<Pair<Integer, String>> visitPermit(Permit permit) {
      return ImmutableList.of(new Pair<>(packet, null));
    }

    @Override
    public List<Pair<Integer, String>> visitRedirect(Redirect redirect) {
      return redirect.getNextHops().stream()
          .map(nhp -> new Pair<>(packet, nhp.getValue()))
          .collect(Collectors.toList());
    }
  }

  public static class OutboundVisitor implements GenericActionVisitor<Integer> {
    final int packet;

    public OutboundVisitor(int packet) {
      this.packet = packet;
    }

    @Override
    public Integer visitActionChain(ActionChain actionChain) {
      if (actionChain.getActions().stream().anyMatch(action -> action instanceof Deny)) {
        return 0;
      } else {
        return BddUtil.orInBatch(
            Controller.bddManager.getBDD(),
            actionChain.getActions().stream()
                .filter(action -> !(action instanceof Deny))
                .map(action -> action.accept(this))
                .collect(Collectors.toList()));
      }
    }

    @Override
    public Integer visitDeny(Deny deny) {
      return 0;
    }

    @Override
    public Integer visitPermit(Permit permit) {
      return packet;
    }

    @Override
    public Integer visitRedirect(Redirect redirect) {
      throw new UnsupportedOperationException(
          "Outbound traffic behaviors do not support redirection");
    }
  }
}
