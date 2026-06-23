package dataplane.analysis;

import datamodel.trafficpolicy.classifier.*;
import jdd.bdd.BDD;
import util.BddUtil;

import java.util.stream.Collectors;

public class TrafficClassifierVisitor implements GenericMatchVisitor<Integer> {
  final BDD bdd;
  final int packet;

  public TrafficClassifierVisitor(BDD bdd, int packet) {
    this.bdd = bdd;
    this.packet = packet;
  }

  @Override
  public Integer visitMatchAcl(MatchAcl matchAcl) {
    int aclPermit = new AclVisitor(bdd).visitAcl(matchAcl.getAcl()).getKey();
    return bdd.ref(bdd.and(aclPermit, packet));
  }

  @Override
  public Integer visitMatchAll(MatchAll matchAll) {
    return BddUtil.andInBatch(
        bdd,
        matchAll.getConjunctions().stream().map(c -> c.accept(this)).collect(Collectors.toList()));
  }

  @Override
  public Integer visitMatchAlways(MatchAlways matchAlways) {
    return matchAlways.isPermit() ? packet : 0;
  }

  @Override
  public Integer visitMatchAny(MatchAny matchAny) {
    return BddUtil.orInBatch(
        bdd,
        matchAny.getDisjunctions().stream().map(d -> d.accept(this)).collect(Collectors.toList()));
  }
}
