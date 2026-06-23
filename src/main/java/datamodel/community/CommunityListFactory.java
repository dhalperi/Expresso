package datamodel.community;

import datamodel.path.BTE;
import dk.brics.automaton.Automaton;
import main.Configuration;
import main.Controller;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommunityListFactory {
  public static CommunityListIntf of(Community... communities) {
    return Configuration.SYMBOLIC_COMMUNITY
        ? (Configuration.SYMBOLIC_COMMUNITY_AP
            ? new DdCommunityList(
                Controller.cpExecutor.communityManager.encodeOnlyOrigins(
                    Arrays.stream(communities)
                        .map(CommunityRegex::from)
                        .collect(Collectors.toSet())))
            : new AutoCommunityList(
                Arrays.stream(communities)
                    .map(CommunityRegex::from)
                    .map(CommunityRegex::toAutomaton)
                    .reduce(Automaton.makeAnyString(), Automaton::intersection)))
        : new CommunityList(Arrays.asList(communities));
  }

  public static CommunityListIntf empty() {
    return Configuration.SYMBOLIC_COMMUNITY
        ? (Configuration.SYMBOLIC_COMMUNITY_AP
            ? new DdCommunityList(Controller.cpExecutor.communityManager.empty())
            : AutoCommunityList.EMPTY)
        : CommunityList.EMPTY;
  }

  public static CommunityListIntf arbitrary() {
    return Configuration.SYMBOLIC_COMMUNITY
        ? (Configuration.SYMBOLIC_COMMUNITY_AP
            ? DdCommunityList.ARBITRARY
            : AutoCommunityList.ARBITRARY)
        : CommunityList.EMPTY;
  }

  public static CommunityListIntf noBTE() {
    return Configuration.SYMBOLIC_COMMUNITY
        ? (Configuration.SYMBOLIC_COMMUNITY_AP
            ? new DdCommunityList(
                Controller.cpExecutor.communityManager.diff(
                    Controller.cpExecutor.communityManager.universe(),
                    Controller.cpExecutor.communityManager.encodeOrigin(BTE.getCommunityRegex())))
            : AutoCommunityList.EMPTY)
        : CommunityList.EMPTY;
  }
}
