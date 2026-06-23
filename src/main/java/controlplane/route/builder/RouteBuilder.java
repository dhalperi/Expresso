package controlplane.route.builder;

import controlplane.network.Interface;
import controlplane.route.Attributes;
import controlplane.route.Route;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;

import java.util.Set;

public abstract class RouteBuilder<B extends RouteBuilder<B, R>, R extends Route> {
    Set<Prefix> _prefixes;
    int _prefixesBdd = Attributes.UNSET_ROUTE_PREFIXES_BDD;

    Ip _nextHopIp = Attributes.UNSET_ROUTE_NEXT_HOP_IP;
    Interface _nextHopInterface = Attributes.UNSET_NEXT_HOP_INTERFACE;

    int _preference = Attributes.UNSET_ROUTE_PREFERENCE;
    int _admin = Attributes.UNSET_ROUTE_ADMIN;

    long _tag = Attributes.UNSET_ROUTE_TAG;
    long _metric = Attributes.UNSET_ROUTE_METRIC;

    public abstract R build();

    protected abstract B getThis();

    public final Set<Prefix> getPrefixes() {
        return _prefixes;
    }

    public final B setPrefixes(Set<Prefix> prefixes) {
        _prefixes = prefixes;
        return getThis();
    }

    public final int getPrefixesBdd() {
        return _prefixesBdd;
    }

    public final B setPrefixesBdd(int prefixesBdd) {
        _prefixesBdd = prefixesBdd;
        return getThis();
    }

    public final Ip getNextHopIp() {
        return _nextHopIp;
    }

    public final B setNextHopIp(Ip nextHopIp) {
        _nextHopIp = nextHopIp;
        return getThis();
    }

    public final Interface getNextHopInterface() {
        return _nextHopInterface;
    }

    public final B setNextHopInterface(Interface nextHopInterface) {
        _nextHopInterface = nextHopInterface;
        return getThis();
    }

    public int getPreference() {
        return _preference;
    }

    public final B setPreference(int preference) {
        _preference = preference;
        return getThis();
    }

    public int getAdmin() {
        return _admin;
    }

    public final B setAdmin(int admin) {
        _admin = admin;
        return getThis();
    }

    public final long getTag() {
        return _tag;
    }

    public final B setTag(long tag) {
        _tag = tag;
        return getThis();
    }

    public final long getMetric() {
        return _metric;
    }

    public final B setMetric(long metric) {
        _metric = metric;
        return getThis();
    }
}
