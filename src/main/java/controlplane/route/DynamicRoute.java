package controlplane.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import controlplane.network.Interface;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.path.Path;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;

import java.util.Set;

public abstract class DynamicRoute<
        B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
    extends Route {
  private static final String PROP_PROPAGATION_PATH = "propagationPath";

  // propagation path
  Path _path;

  public DynamicRoute(
      Set<Prefix> prefixes,
      int prefixesBdd,
      Ip nextHopIp,
      Interface nextHopInterface,
      int preference,
      int admin,
      long tag,
      Path path) {
    super(prefixes, prefixesBdd, nextHopIp, nextHopInterface, preference, admin, tag);
    _path = path;
  }

  @JsonProperty(PROP_PROPAGATION_PATH)
  public Path getPath() {
    return _path;
  }

  public void setPath(Path path) {
    _path = path;
  }

  @Override
  public abstract B toBuilder();

  @Override
  public MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("path", _path);
  }
}
