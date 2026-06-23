package controlplane.rib;

import controlplane.route.BgpRoute;
import javafx.util.Pair;
import main.Controller;

import java.util.Map;

public class BgpRib extends Rib<BgpRoute> {
    boolean additionalPath;
    boolean iBgpMultipath;
    boolean eBgpMultipath;

    public BgpRib(boolean additionalPath, boolean iBgpMultipath, boolean eBgpMultipath) {
        super();
        this.additionalPath = additionalPath;
        this.iBgpMultipath = iBgpMultipath;
        this.eBgpMultipath = eBgpMultipath;
    }

    @Override
    public void update() {
        int matched = 0;
        for (Map.Entry<BgpRoute, RibRouteSet<BgpRoute>> entry : rib.entrySet()) {
            if (additionalPath) {
                matched = additionalUpdate(matched, entry.getValue());
            }
            else if (entry.getKey().getType() == BgpRoute.BgpRouteType.FROM_EBGP_NEIGHBOR) {
                matched = eBgpMultipath ? multipathUpdate(matched, entry.getValue()) : bestpathUpdate(matched, entry.getValue());
            }
            else {
                matched = iBgpMultipath ? multipathUpdate(matched, entry.getValue()) : bestpathUpdate(matched, entry.getValue());
            }
        }
    }

    public int additionalUpdate(int matched, RibRouteSet<BgpRoute> equalCostRoutes) {
        return super.multipathUpdate(matched, equalCostRoutes);
    }

    public int multipathUpdate(int matched, RibRouteSet<BgpRoute> equalCostRoutes) {
        return super.multipathUpdate(matched, equalCostRoutes);
    }

    public int bestpathUpdate(int matched, RibRouteSet<BgpRoute> equalCostRoutes) {
        for (Pair<BgpRoute, RouteState> pair : equalCostRoutes.getAllRoutesWithState()) {
            BgpRoute route = pair.getKey();
            RouteState state = pair.getValue();

            // compute new prefixesBdd
            int newBdd = 0;
            if (matched != 1)  {
                newBdd = Controller.bddManager.and(route.getPrefixesBdd(), Controller.bddManager.not(matched));
            }
            matched = Controller.bddManager.or(matched, route.getPrefixesBdd());

            // compute new state
            RouteState newState = state;
            if (state == RouteState.INSERTED) {
                if (newBdd == 0) {
                    newState = RouteState.EMPTY;
                }
            }
            else if (state != RouteState.REPLACED) {
                newState = route.getPrefixesBdd() != newBdd ? RouteState.UPDATED : RouteState.UNCHANGED;
            }

            route.setPrefixesBdd(newBdd);
            equalCostRoutes.setState(route.getAttributes(), newState);
        }
        return matched;
    }
}
