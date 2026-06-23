package dataplane.analysis;

import datamodel.acl.*;
import javafx.util.Pair;
import jdd.bdd.BDD;
import main.Controller;
import util.BddUtil;

import java.util.stream.Collectors;

public class AclVisitor implements GenericAclVisitor<Pair<Integer, Integer>> {
  BDD bdd;

  public AclVisitor(BDD bdd) {
    this.bdd = bdd;
  }

  /**
   * <a
   * href="https://support.huawei.com/enterprise/en/doc/EDOC1000178177?section=j00c">Huawei-doc</a>
   * For ACLs used in Huawei's traffic classifiers: <br>
   * 1. the default ACL action is permit. <br>
   * 2. packets matching the permit rule. <br>
   * 3. packets matching the deny rule is denied (discarded). <br>
   * 4. packets do not match any rule in an acl are permitted. <br>
   * 5. an ACL that doesn't contain any rule permits all packets. <br>
   */
  @Override
  public Pair<Integer, Integer> visitAcl(Acl acl) {
    if (acl.getLines().isEmpty()) {
      // acls containing no rules permit all packets by default.
      return new Pair<>(1, 0);
    } else {
      AclLineMatchVisitor lineMatchVisitor = new AclLineMatchVisitor(bdd, this);
      int permitBdd = 0;
      int denyBdd = 0;
      int tmp;
      for (AclLine line : acl.getLines()) {
        // todo
        Pair<Integer, Integer> lineMatchResult = lineMatchVisitor.visit(line.getMatch());
        int linePermit =
            line.getAction() == AclLineAction.PERMIT
                ? lineMatchResult.getKey()
                : lineMatchResult.getValue();
        int lineDeny =
            line.getAction() == AclLineAction.PERMIT
                ? lineMatchResult.getValue()
                : lineMatchResult.getKey();
        // not denied previously
        int permit = Controller.bddManager.minus1(linePermit, denyBdd);
        tmp = Controller.bddManager.or1(permitBdd, permit);
        Controller.bddManager.deref(permitBdd);
        permitBdd = tmp;

        tmp = Controller.bddManager.or1(denyBdd, lineDeny);
        Controller.bddManager.deref(denyBdd);
        denyBdd = tmp;
      }
      // packets not matching any rule are denied by default.
      tmp = Controller.bddManager.not1(permitBdd);
      Controller.bddManager.deref(denyBdd);
      denyBdd = tmp;

      return new Pair<>(permitBdd, denyBdd);
    }
  }

  public static class AclLineMatchVisitor
      implements GenericAclLineMatchVisitor<Pair<Integer, Integer>> {
    BDD bdd;
    AclVisitor aclVisitor;

    public AclLineMatchVisitor(BDD bdd, AclVisitor aclVisitor) {
      this.bdd = bdd;
      this.aclVisitor = aclVisitor;
    }

    @Override
    public Pair<Integer, Integer> visitAclLineMatchAcl(AclLineMatchAcl aclLineMatchAcl) {
      return aclVisitor.visitAcl(aclLineMatchAcl.getReferenceAcl());
    }

    @Override
    public Pair<Integer, Integer> visitAclLineMatchAnd(AclLineMatchAnd aclLineMatchAnd) {
      int permit =
          BddUtil.andInBatch(
              bdd,
              aclLineMatchAnd.getConjunctions().stream()
                  .map(c -> c.accept(this).getKey())
                  .collect(Collectors.toList()));
      int deny = bdd.not(permit);
      return new Pair<>(permit, deny);
    }

    @Override
    public Pair<Integer, Integer> visitAclLineMatchOr(AclLineMatchOr aclLineMatchOr) {
      int permit =
          BddUtil.orInBatch(
              bdd,
              aclLineMatchOr.getDisjunctions().stream()
                  .map(c -> c.accept(this).getKey())
                  .collect(Collectors.toList()));
      int deny = bdd.not(permit);
      return new Pair<>(permit, deny);
    }

    @Override
    public Pair<Integer, Integer> visitAclLineMatchPacketHeader(
        AclLineMatchPacketHeader aclLineMatchPacketHeader) {
      // todo
      // only encode destination IP now
      int permit =
          Controller.bddManager
              .getBddPrefixWrapper()
              .encodePrefix(aclLineMatchPacketHeader.getDestinationIp());
      int deny = Controller.bddManager.not1(permit);
      return new Pair<>(permit, deny);
    }
  }
}
