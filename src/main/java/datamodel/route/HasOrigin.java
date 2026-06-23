package datamodel.route;

import controlplane.route.BgpRoute;

public interface HasOrigin {
    BgpRoute.OriginType getOrigin();

    void setOrigin(BgpRoute.OriginType origin);
}
