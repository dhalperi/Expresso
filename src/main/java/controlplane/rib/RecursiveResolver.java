package controlplane.rib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import controlplane.network.Interface;
import controlplane.route.Route;
import datamodel.ipv4.Ip;
import datamodel.longestprefixmatch.LongestPrefixMatch;
import main.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RecursiveResolver {
    private static final int MAX_DEPTH = 10;

    public static <R extends Route> Set<ResolutionResult> resolveRoute(Route route, Rib<R> rib) {
        ResolutionTreeNode resolutionRoot = ResolutionTreeNode.root(route);
        buildResolutionTree(
                route,
                rib,
                Ip.AUTO,
                0,
                0,
                resolutionRoot);
        Set<ResolutionResult> collector = new HashSet<>();
        collect(resolutionRoot, new Stack<>(), collector);
        return collector;
    }

    public static void collect(ResolutionTreeNode node, Stack<Route> stack, Set<ResolutionResult> collector) {
        if (node.getChildren().isEmpty() && node.getFinalNextHopIp() != null) {
            collector.add(new ResolutionResult(node._finalNextHopIp, ImmutableList.copyOf(stack)));
            return;
        }
        stack.push(node._route);
        for (ResolutionTreeNode child : node.getChildren()) {
            collect(child, stack, collector);
        }
        stack.pop();
    }

    public static <R extends Route> boolean buildResolutionTree(
            Route route,
            Rib<R> rib,
            Ip mostRecentNextHopIp,
            int seenNetworks,
            int depth,
            ResolutionTreeNode parentNode
    ) {
        int notSeenNetworks = Controller.bddManager.minus(route.getPrefixesBdd(), seenNetworks);
        if (notSeenNetworks == 0 || depth > MAX_DEPTH) {
            // Don't enter a resolution loop
            return false;
        }

        Interface nextHopInterface = route.getNextHopInterface();
        Ip nextHopIp = route.getNextHopIp();
        // Finally, we have reached an interface route.
        if (nextHopInterface != null) {
            // Discard
            if (nextHopInterface.isBlackhole()) {
                ResolutionTreeNode.withParent(route, parentNode, Ip.AUTO);
                return false;
            } else {
                // nextHop equals Ip.AUTO means this is a connected route.
                // otherwise, ospf route.
                ResolutionTreeNode.withParent(route, parentNode, nextHopIp.equals(Ip.AUTO) ? mostRecentNextHopIp : nextHopIp);
                return true;
            }
        } else {

            int newSeenNetworks = depth == 0 ? seenNetworks : Controller.bddManager.or(seenNetworks, route.getPrefixesBdd());

            boolean stop = false;
            for (int preLen = 32; preLen >= 0 && !stop; preLen--) {
                List<R> matched = LongestPrefixMatch.matches(rib.getAllRoutes(), nextHopIp, preLen);
                if (matched != null && !matched.isEmpty()) {
                    // We have at least one valid longest-prefix match
                    for (R matchedRoute : matched) {
                        stop = buildResolutionTree(
                                matchedRoute,
                                rib,
                                nextHopIp,
                                newSeenNetworks,
                                depth + 1,
                                ResolutionTreeNode.withParent(matchedRoute, parentNode, null)
                        ) | stop;
                    }
                }
            }
            return stop;
        }
    }

    /**
     * From batfish FibImpl: Helps perform recursive route resolution and maintain the route chain
     */
    private static final class ResolutionTreeNode {
        private final @Nonnull Route _route;
        private final @Nullable Ip _finalNextHopIp;

        private final @Nonnull List<ResolutionTreeNode> _children;

        /**
         * Use static factories for sanity
         */
        private ResolutionTreeNode(@Nonnull Route route, @Nullable Ip finalNextHopIp, @Nonnull List<ResolutionTreeNode> children) {
            _route = route;
            _finalNextHopIp = finalNextHopIp;
            _children = children;
        }

        static ResolutionTreeNode withParent(Route route, @Nullable ResolutionTreeNode parent, @Nullable Ip finalNextHopIp) {
            ResolutionTreeNode child = new ResolutionTreeNode(route, finalNextHopIp, new LinkedList<>());
            if (parent != null) {
                parent.addChild(child);
            }
            return child;
        }

        static ResolutionTreeNode root(@Nonnull Route route) {
            return new ResolutionTreeNode(route, null, new LinkedList<>());
        }

        @Nonnull
        public Route getRoute() {
            return _route;
        }

        @Nullable
        public Ip getFinalNextHopIp() {
            return _finalNextHopIp;
        }

        @Nonnull
        public List<ResolutionTreeNode> getChildren() {
            return _children;
        }

        private void addChild(ResolutionTreeNode child) {
            _children.add(child);
        }
    }

    public static class ResolutionResult {
        final Ip finalNextHop;
        final List<Route> resolutionSteps;

        public ResolutionResult(Ip finalNextHop, List<Route> resolutionSteps) {
            this.finalNextHop = finalNextHop;
            this.resolutionSteps = resolutionSteps;
        }

        public Ip getFinalNextHopIp() {
            return finalNextHop;
        }

        public Interface getFinalNextHopInterface() {
            return Iterables.getLast(resolutionSteps).getNextHopInterface();
        }

        public List<Route> getResolutionSteps() {
            return resolutionSteps;
        }
    }
}
