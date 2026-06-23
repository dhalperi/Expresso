package datamodel.tag;

import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;

import java.util.Objects;

public class Tag implements CanBeMatched {
  private final long tag;

  public Tag(long tag) {
    this.tag = tag;
  }

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);
    if (route.getTag() == tag) {
      result.getPermitted().add(route.toBuilder().build());
    } else {
      result.getDenied().add(route.toBuilder().build());
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tag tag1 = (Tag) o;
    return tag == tag1.tag;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag);
  }
}
