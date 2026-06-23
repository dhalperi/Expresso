package inputparser.visitors;

import controlplane.network.Router;
import controlplane.route.BgpRoute;
import datamodel.ipv4.Ip;
import datamodel.routepolicy.action.Action;
import datamodel.routepolicy.action.SetAsPath;
import datamodel.routepolicy.action.SetCost;
import datamodel.routepolicy.action.SetPreference;
import datamodel.routepolicy.action.StaticAction;
import inputparser.bfvi.RoutePolicyParserHelper;
import javafx.util.Pair;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.expr.BgpPeerAddressNextHop;
import org.batfish.datamodel.routing_policy.expr.DiscardNextHop;
import org.batfish.datamodel.routing_policy.expr.IpNextHop;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.NextHopExpr;
import org.batfish.datamodel.routing_policy.expr.OriginExpr;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.UnchangedNextHop;
import org.batfish.datamodel.routing_policy.statement.*;

import java.util.List;

import static inputparser.bfvi.RoutePolicyParserHelper.ACCEPT;
import static inputparser.bfvi.RoutePolicyParserHelper.REJECT;
import static inputparser.bfvi.RoutePolicyParserHelper.getAsPath;

public class StatementToAction implements StatementVisitor<Action, Router> {
  IntVisitor intVisitor = new IntVisitor();
  LongVisitor longVisitor = new LongVisitor();
  CommunitySetToAction communitySetToAction = new CommunitySetToAction();

  @Override
  public Action visitAddCommunity(AddCommunity addCommunity, Router router) {
    return addCommunity.getExpr().accept(new ExprCommunitySetToAction(router));
  }

  @Override
  public Action visitBufferedStatement(BufferedStatement bufferedStatement, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitCallStatement(CallStatement callStatement, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitComment(Comment comment, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitDeleteCommunity(DeleteCommunity deleteCommunity, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitIf(If anIf, Router router) { // todo
    if (anIf.equals(ACCEPT)) {
      return StaticAction.ExitAccept;
    }
    if (anIf.equals(REJECT)) {
      return StaticAction.ExitReject;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitOverwriteAsPath(OverwriteAsPath overwriteAsPath, Router router) {
    List<Long> asns = getAsPath(overwriteAsPath.getExpr());
    return SetAsPath.overwrite(asns);
  }

  @Override
  public Action visitPrependAsPath(PrependAsPath prependAsPath, Router router) {
    List<Long> asns = getAsPath(prependAsPath.getExpr());
    return SetAsPath.additive(asns);
  }

  @Override
  public Action visitSetAdministrativeCost(
      SetAdministrativeCost setAdministrativeCost, Router router) {
    int admin = setAdministrativeCost.getAdmin().accept(intVisitor, router);
    return new SetPreference(admin);
  }

  @Override
  public Action visitSetCommunities(SetCommunities setCommunities, Router router) {
    return setCommunities.getCommunitySetExpr().accept(communitySetToAction, router);
  }

  @Override
  public Action visitSetCommunity(SetCommunity setCommunity, Router router) {
    return setCommunity.getExpr().accept(new ExprCommunitySetToAction(router));
  }

  @Override
  public Action visitSetDefaultPolicy(SetDefaultPolicy setDefaultPolicy, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitSetEigrpMetric(SetEigrpMetric setEigrpMetric, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitSetIsisLevel(SetIsisLevel setIsisLevel, Router router) {
    // todo
    return null;
  }

  @Override
  public Action visitSetIsisMetricType(SetIsisMetricType setIsisMetricType, Router router) {
    // todo
    return null;
  }

  @Override
  public Action visitSetLocalPreference(SetLocalPreference setLocalPreference, Router router) {
    Pair<Long, RoutePolicyParserHelper.SetType> pair =
        setLocalPreference.getLocalPreference().accept(longVisitor, router);
    switch (pair.getValue()) {
      case INCREMENT:
        return datamodel.routepolicy.action.SetLocalPreference.increase(pair.getKey().intValue());
      case DECREMENT:
        return datamodel.routepolicy.action.SetLocalPreference.decrease(pair.getKey().intValue());
      default:
        return datamodel.routepolicy.action.SetLocalPreference.replace(pair.getKey().intValue());
    }
  }

  @Override
  public Action visitSetMetric(SetMetric setMetric, Router router) {
    Pair<Long, RoutePolicyParserHelper.SetType> pair =
        setMetric.getMetric().accept(longVisitor, router);
    switch (pair.getValue()) {
      case INCREMENT:
        return SetCost.increase(pair.getKey().intValue());
      case DECREMENT:
        return SetCost.decrease(pair.getKey().intValue());
      default:
        return SetCost.replace(pair.getKey().intValue());
    }
  }

  @Override
  public Action visitSetNextHop(SetNextHop setNextHop, Router router) {
    NextHopExpr nextHopExpr = setNextHop.getExpr();
    if (nextHopExpr instanceof IpNextHop) {
      return datamodel.routepolicy.action.SetNextHop.ip(
          router, Ip.of(((IpNextHop) nextHopExpr).getIps().get(0).asLong()));
    } else if (nextHopExpr instanceof DiscardNextHop) {
      return datamodel.routepolicy.action.SetNextHop.blackhole(router);
    } else if (nextHopExpr instanceof BgpPeerAddressNextHop) {
      return datamodel.routepolicy.action.SetNextHop.peerAddress(router);
    } else if (nextHopExpr instanceof UnchangedNextHop) {
      return null;
    } else if (nextHopExpr instanceof SelfNextHop) {
      return datamodel.routepolicy.action.SetNextHop.self(router);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Action visitSetOrigin(SetOrigin setOrigin, Router router) {
    OriginExpr originExpr = setOrigin.getOriginType();
    if (originExpr instanceof LiteralOrigin) {
      return new datamodel.routepolicy.action.SetOrigin(
          null,
          BgpRoute.OriginType.valueOf(((LiteralOrigin) originExpr).getOriginType().toString()));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Action visitSetOspfMetricType(SetOspfMetricType setOspfMetricType, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitSetTag(SetTag setTag, Router router) {
    return new datamodel.routepolicy.action.SetTag(
        setTag.getTag().accept(longVisitor, router).getKey());
  }

  @Override
  public Action visitSetVarMetricType(SetVarMetricType setVarMetricType, Router router) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Action visitSetWeight(SetWeight setWeight, Router router) {
    int w = setWeight.getWeight().accept(intVisitor, router);
    return new datamodel.routepolicy.action.SetWeight(w);
  }

  @Override
  public Action visitStaticStatement(Statements.StaticStatement staticStatement, Router router) {
    switch (staticStatement.getType()) {
      case ExitAccept:
        return StaticAction.ExitAccept;
      case ReturnTrue:
        return StaticAction.ReturnTrue;
      case ExitReject:
        return StaticAction.ExitReject;
      case ReturnFalse:
        return StaticAction.ReturnFalse;
      case Return:
        return StaticAction.Return;
      case FallThrough:
        return StaticAction.FallThrough;
      default:
        throw new UnsupportedOperationException(
            String.format(
                "Haven't support static statement %s until now.", staticStatement.getType()));
    }
  }
}
