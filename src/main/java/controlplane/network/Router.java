package controlplane.network;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import datamodel.filterlist.FilterList;
import datamodel.filterlist.FilterType;
import datamodel.routepolicy.RoutePolicy;
import datamodel.acl.Acl;
import datamodel.trafficpolicy.TrafficPolicy;
import datamodel.trafficpolicy.behavior.TrafficBehavior;
import datamodel.trafficpolicy.classifier.TrafficClassifier;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Router {
  String routerName;

  SortedMap<InterfaceName, Interface> interfaces;
  Interface blackhole;

  SortedMap<String, VirtualRouter> virtualRouters;
  Table<FilterType, String, FilterList> filterLists;
  SortedMap<String, RoutePolicy> routePolicies;

  SortedMap<String, Acl> acls;

  SortedMap<String, TrafficClassifier> trafficClassifiers;
  SortedMap<String, TrafficBehavior> trafficBehaviors;
  SortedMap<String, TrafficPolicy> trafficPolicies;

  public Router(String name) {
    routerName = name.toLowerCase(); // use lower case names to keep consistent with Batfish
    interfaces = new TreeMap<>();
    virtualRouters = new TreeMap<>();
    filterLists = HashBasedTable.create();
    routePolicies = new TreeMap<>();
    acls = new TreeMap<>();
    trafficClassifiers = new TreeMap<>();
    trafficBehaviors = new TreeMap<>();
    trafficPolicies = new TreeMap<>();
  }

  @JsonValue
  public String getRouterName() {
    return routerName;
  }

  public Map<InterfaceName, Interface> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(SortedMap<InterfaceName, Interface> interfaces) {
    this.interfaces = interfaces;
  }

  public SortedMap<String, VirtualRouter> getVirtualRouters() {
    return virtualRouters;
  }

  public void setVirtualRouters(SortedMap<String, VirtualRouter> virtualRouters) {
    this.virtualRouters = virtualRouters;
  }

  public Table<FilterType, String, FilterList> getFilterLists() {
    return filterLists;
  }

  public void setFilterLists(Table<FilterType, String, FilterList> filterLists) {
    this.filterLists = filterLists;
  }

  public SortedMap<String, RoutePolicy> getRoutePolicies() {
    return routePolicies;
  }

  public void setRoutePolicies(SortedMap<String, RoutePolicy> routePolicies) {
    this.routePolicies = routePolicies;
  }

  public SortedMap<String, Acl> getAcls() {
    return acls;
  }

  public void setAcls(SortedMap<String, Acl> acls) {
    this.acls = acls;
  }

  public SortedMap<String, TrafficClassifier> getTrafficClassifiers() {
    return trafficClassifiers;
  }

  public void setTrafficClassifiers(SortedMap<String, TrafficClassifier> trafficClassifiers) {
    this.trafficClassifiers = trafficClassifiers;
  }

  public SortedMap<String, TrafficBehavior> getTrafficBehaviors() {
    return trafficBehaviors;
  }

  public void setTrafficBehaviors(SortedMap<String, TrafficBehavior> trafficBehaviors) {
    this.trafficBehaviors = trafficBehaviors;
  }

  public SortedMap<String, TrafficPolicy> getTrafficPolicies() {
    return trafficPolicies;
  }

  public void setTrafficPolicies(SortedMap<String, TrafficPolicy> trafficPolicies) {
    this.trafficPolicies = trafficPolicies;
  }

  @Override
  public String toString() {
    return String.format("Router{%s}", routerName);
  }

  /* Following are helper functions when parsing configurations */

  /**
   * Helper function to get an {@link Interface}.
   *
   * @param name the interface name of the interface, not the full name. See {@link InterfaceName}.
   * @return {@link Interface}
   */
  public Interface getInterface(String name) {
    return name == null ? null : interfaces.get(new InterfaceName(routerName, name));
  }

  /** If name is null, return null; */
  public FilterList getFilterList(FilterType type, String name) {
    FilterList ret = name == null ? null : filterLists.get(type, name);
    if (ret != null) ret.setUsed(true);
    return ret;
  }

  /** If name is null, return null; */
  public RoutePolicy getRoutePolicy(String name) {
    RoutePolicy ret =
        name == null ? null : routePolicies.getOrDefault(name, RoutePolicy.PERMIT_ALL);
    if (ret != null) ret.setUsed(true);
    return ret;
  }

  /** If name is null, return null; */
  public Acl getAcl(String name) {
    Acl ret = name == null ? null : acls.getOrDefault(name, Acl.PERMIT_ALL);
    if (ret != null) ret.setUsed(true);
    return ret;
  }

  public TrafficClassifier getTrafficClassifier(String name) {
    return name == null ? null : trafficClassifiers.getOrDefault(name, TrafficClassifier.NONE);
  }

  public TrafficBehavior getTrafficBehavior(String name) {
    return name == null ? null : trafficBehaviors.getOrDefault(name, TrafficBehavior.PERMIT);
  }

  public TrafficPolicy getTrafficPolicy(String name) {
    TrafficPolicy ret =
        name == null ? null : trafficPolicies.getOrDefault(name, TrafficPolicy.PERMIT_ALL);
    if (ret != null) ret.setUsed(true);
    return ret;
  }

  SortedMap<String, CommunityList> communityLists;
  SortedMap<String, CommunityMatchExpr> communityMatchExpr;
  SortedMap<String, CommunitySetExpr> communitySetExprs;
  SortedMap<String, CommunitySetMatchExpr> communitySetMatchExpr;
  SortedMap<String, CommunitySet> communitySets;

  public SortedMap<String, CommunityList> getCommunityLists() {
    return communityLists;
  }

  public void setCommunityLists(SortedMap<String, CommunityList> communityLists) {
    this.communityLists = communityLists;
  }

  public SortedMap<String, CommunityMatchExpr> getCommunityMatchExpr() {
    return communityMatchExpr;
  }

  public void setCommunityMatchExpr(SortedMap<String, CommunityMatchExpr> communityMatchExpr) {
    this.communityMatchExpr = communityMatchExpr;
  }

  public SortedMap<String, CommunitySetExpr> getCommunitySetExprs() {
    return communitySetExprs;
  }

  public void setCommunitySetExprs(SortedMap<String, CommunitySetExpr> communitySetExprs) {
    this.communitySetExprs = communitySetExprs;
  }

  public SortedMap<String, CommunitySetMatchExpr> getCommunitySetMatchExpr() {
    return communitySetMatchExpr;
  }

  public void setCommunitySetMatchExpr(
      SortedMap<String, CommunitySetMatchExpr> communitySetMatchExpr) {
    this.communitySetMatchExpr = communitySetMatchExpr;
  }

  public SortedMap<String, CommunitySet> getCommunitySets() {
    return communitySets;
  }

  public void setCommunitySets(SortedMap<String, CommunitySet> communitySets) {
    this.communitySets = communitySets;
  }
}
