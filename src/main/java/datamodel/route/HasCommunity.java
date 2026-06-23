package datamodel.route;

import datamodel.community.CommunityListIntf;

public interface HasCommunity {
    CommunityListIntf getCommunities();

    void setCommunities(CommunityListIntf communities);
}
