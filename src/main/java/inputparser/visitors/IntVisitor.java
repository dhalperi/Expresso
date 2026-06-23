package inputparser.visitors;

import controlplane.network.Router;
import org.batfish.datamodel.routing_policy.expr.IntExprVisitor;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.VarInt;

public class IntVisitor implements IntExprVisitor<Integer, Router> {
  @Override
  public Integer visitLiteralInt(LiteralInt literalInt, Router router) {
    return literalInt.getValue();
  }

  @Override
  public Integer visitVarInt(VarInt varInt, Router router) {
    throw new UnsupportedOperationException("Haven't support VarInt until now.");
  }
}
