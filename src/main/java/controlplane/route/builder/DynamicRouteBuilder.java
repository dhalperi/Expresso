package controlplane.route.builder;

import controlplane.route.DynamicRoute;
import datamodel.path.Path;

public abstract class DynamicRouteBuilder<B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>> extends RouteBuilder<B, R> {
    // propagation path
    Path _path = new Path();

    public abstract R build();

    public final Path getPath() {
        return _path;
    }

    public final B setPath(Path path) {
        _path = path;
        return getThis();
    }
}
