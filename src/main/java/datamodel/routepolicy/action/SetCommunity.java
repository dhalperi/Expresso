package datamodel.routepolicy.action;

import controlplane.route.Route;
import datamodel.aspath.AsPathRegex;
import datamodel.community.Community;
import datamodel.community.CommunityListIntf;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasCommunity;
import datamodel.route.RouteFilterEnvironment;
import dk.brics.automaton.Automaton;
import main.Controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SetCommunity implements Action {
  private final boolean increase;
  private final boolean decrease;
  private final List<Community> communities;

  Automaton automaton;
  int dd = -1;

  private SetCommunity(boolean increase, boolean decrease, Community... communities) {
    this(increase, decrease, Arrays.asList(communities));
  }

  private SetCommunity(boolean increase, boolean decrease, List<Community> communities) {
    if (increase && decrease) {
      throw new IllegalArgumentException("Cannot be incremental and decremental at the same time.");
    }
    this.increase = increase;
    this.decrease = decrease;
    this.communities = communities;
  }

  public static SetCommunity replace(Community... communities) {
    return new SetCommunity(false, false, communities);
  }

  public static SetCommunity replace(List<Community> communities) {
    return new SetCommunity(false, false, communities);
  }

  public static SetCommunity increase(Community... communities) {
    return new SetCommunity(true, false, communities);
  }

  public static SetCommunity increase(List<Community> communities) {
    return new SetCommunity(true, false, communities);
  }

  public static SetCommunity decrease(Community... communities) {
    return new SetCommunity(false, true, communities);
  }

  public static SetCommunity decrease(List<Community> communities) {
    return new SetCommunity(false, true, communities);
  }

  public List<Community> getCommunities() {
    return communities;
  }

  public Automaton getAutomaton() {
    if (automaton == null) {
      automaton =
          communities.stream()
              .map(comm -> CommunityRegex.from(comm).toListAutomaton())
              .reduce(Automaton.makeAnyString(), Automaton::intersection);
    }
    return automaton;
  }

  private void initDd() {
    dd =
        Controller.cpExecutor.communityManager.encodeOrigins(
            communities.stream().map(CommunityRegex::from).collect(Collectors.toSet()));
  }

  public int getDd() {
    if (dd == -1) initDd();
    return dd;
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasCommunity) {
      HasCommunity has = (HasCommunity) route;
      CommunityListIntf communityList =
          increase
              ? has.getCommunities().addCommunities(this)
              : (decrease
                  ? has.getCommunities().deleteCommunities(this)
                  : has.getCommunities().setCommunities(this));
      has.setCommunities(communityList);
    }
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return Collections.emptySet();
  }

  Set<CommunityRegex> communityRegexes;

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    if (communityRegexes == null)
      communityRegexes = communities.stream().map(CommunityRegex::from).collect(Collectors.toSet());
    return communityRegexes;
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return Collections.emptySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetCommunity that = (SetCommunity) o;
    return increase == that.increase
        && decrease == that.decrease
        && Objects.equals(communities, that.communities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(increase, decrease, communities);
  }
}
