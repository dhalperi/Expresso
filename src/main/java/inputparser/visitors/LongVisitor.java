package inputparser.visitors;

import controlplane.network.Router;
import inputparser.bfvi.RoutePolicyParserHelper;
import javafx.util.Pair;
import org.batfish.datamodel.routing_policy.expr.AsnValue;
import org.batfish.datamodel.routing_policy.expr.DecrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.DecrementMetric;
import org.batfish.datamodel.routing_policy.expr.IgpCost;
import org.batfish.datamodel.routing_policy.expr.IncrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.IncrementMetric;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LongExprVisitor;
import org.batfish.datamodel.routing_policy.expr.Uint32HighLowExpr;
import org.batfish.datamodel.routing_policy.expr.VarLong;

public class LongVisitor implements LongExprVisitor<Pair<Long, RoutePolicyParserHelper.SetType>, Router> {
  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitAsnValue(AsnValue asnValue, Router router) {
    return null;
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitDecrementLocalPreference(
      DecrementLocalPreference decrementLocalPreference, Router router) {
    return new Pair<>(
        decrementLocalPreference.getSubtrahend(), RoutePolicyParserHelper.SetType.DECREMENT);
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitDecrementMetric(
      DecrementMetric decrementMetric, Router router) {
    return new Pair<>(decrementMetric.getSubtrahend(), RoutePolicyParserHelper.SetType.DECREMENT);
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitIgpCost(IgpCost igpCost, Router router) {
    return null;
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitIncrementLocalPreference(
      IncrementLocalPreference incrementLocalPreference, Router router) {
    return new Pair<>(incrementLocalPreference.getAddend(), RoutePolicyParserHelper.SetType.INCREMENT);
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitIncrementMetric(
      IncrementMetric incrementMetric, Router router) {
    return new Pair<>(incrementMetric.getAddend(), RoutePolicyParserHelper.SetType.INCREMENT);
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitLiteralLong(
      LiteralLong literalLong, Router router) {
    return new Pair<>(literalLong.getValue(), RoutePolicyParserHelper.SetType.NONE);
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitUint32HighLowExpr(
      Uint32HighLowExpr uint32HighLowExpr) {
    return null;
  }

  @Override
  public Pair<Long, RoutePolicyParserHelper.SetType> visitVarLong(VarLong varLong, Router router) {
    return null;
  }
}
