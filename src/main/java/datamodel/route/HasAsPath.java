package datamodel.route;

import datamodel.aspath.AsPathIntf;

public interface HasAsPath {
    AsPathIntf getAsPath();

    void setAsPath(AsPathIntf asPath);
}
