package datamodel.community;

import com.fasterxml.jackson.annotation.JsonValue;
import datamodel.routepolicy.action.SetCommunity;
import javafx.util.Pair;

public interface CommunityListIntf {
  CommunityListIntf setCommunities(SetCommunity setCommunity);

  CommunityListIntf addCommunities(SetCommunity setCommunity);

  CommunityListIntf deleteCommunities(SetCommunity setCommunity);

  @JsonValue
  String toString();

  Pair<CommunityListIntf, CommunityListIntf> match(CommunityRegex communityRegex);
}
