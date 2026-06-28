package datamodel.route;

import controlplane.network.Router;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.route.Route;

public class RouteFilterEnvironment<R extends Route> {
  private final Router router;
  private final boolean in;
  private final BgpPeerConfig localConfig;
  private String defaultPolicy;
  private boolean defaultAction;
  // Per-policy "local" default action (Batfish Environment#getLocalDefaultAction): the value
  // returned by ReturnLocalDefaultAction, which models a Cisco route-map falling off the end of its
  // clauses. Defaults to false (route-map fall-off = deny) and is saved/restored around policy
  // calls, mirroring org.batfish.datamodel.routing_policy.expr.CallExpr.
  private boolean localDefaultAction;
  private boolean callExprContext;
  private boolean callStatementContext;
  private RouteFilterResult<R> result;

  private RouteFilterEnvironment(
      Router router,
      boolean in,
      BgpPeerConfig localConfig,
      String defaultPolicy,
      boolean defaultAction,
      boolean localDefaultAction,
      boolean callExprContext,
      boolean callStatementContext,
      RouteFilterResult<R> result) {
    this.router = router;
    this.in = in;
    this.localConfig = localConfig;
    this.defaultPolicy = defaultPolicy;
    this.defaultAction = defaultAction;
    this.localDefaultAction = localDefaultAction;
    this.callExprContext = callExprContext;
    this.callStatementContext = callStatementContext;
    this.result = result;
  }

  public Router getRouter() {
    return router != null
        ? router
        : (localConfig != null ? localConfig.getProcess().getVrf().getRouter() : null);
  }

  public boolean isIn() {
    return in;
  }

  public BgpPeerConfig getLocalConfig() {
    return localConfig;
  }

  public String getDefaultPolicy() {
    return defaultPolicy;
  }

  public void setDefaultPolicy(String defaultPolicy) {
    this.defaultPolicy = defaultPolicy;
  }

  public boolean isDefaultAction() {
    return defaultAction;
  }

  public void setDefaultAction(boolean defaultAction) {
    this.defaultAction = defaultAction;
  }

  public boolean isLocalDefaultAction() {
    return localDefaultAction;
  }

  public void setLocalDefaultAction(boolean localDefaultAction) {
    this.localDefaultAction = localDefaultAction;
  }

  public boolean isCallExprContext() {
    return callExprContext;
  }

  public void setCallExprContext(boolean callExprContext) {
    this.callExprContext = callExprContext;
  }

  public boolean isCallStatementContext() {
    return callStatementContext;
  }

  public void setCallStatementContext(boolean callStatementContext) {
    this.callStatementContext = callStatementContext;
  }

  public RouteFilterResult<R> getResult() {
    return result;
  }

  public void setResult(RouteFilterResult<R> result) {
    this.result = result;
  }

  public static final class Builder<R extends Route> {
    private Router router;
    private boolean in;
    private BgpPeerConfig localConfig;
    private String defaultPolicy;
    private boolean defaultAction;
    private boolean localDefaultAction;
    private boolean callExprContext;
    private boolean callStatementContext;
    private RouteFilterResult<R> result;

    public Builder<R> setRouter(Router router) {
      this.router = router;
      return this;
    }

    public Builder<R> setIn(boolean in) {
      this.in = in;
      return this;
    }

    public Builder<R> setLocalConfig(BgpPeerConfig localConfig) {
      this.localConfig = localConfig;
      return this;
    }

    public Builder<R> setDefaultPolicy(String defaultPolicy) {
      this.defaultPolicy = defaultPolicy;
      return this;
    }

    public Builder<R> setDefaultAction(boolean defaultAction) {
      this.defaultAction = defaultAction;
      return this;
    }

    public Builder<R> setLocalDefaultAction(boolean localDefaultAction) {
      this.localDefaultAction = localDefaultAction;
      return this;
    }

    public Builder<R> setCallExprContext(boolean callExprContext) {
      this.callExprContext = callExprContext;
      return this;
    }

    public Builder<R> setCallStatementContext(boolean callStatementContext) {
      this.callStatementContext = callStatementContext;
      return this;
    }

    public Builder<R> setResult(RouteFilterResult<R> result) {
      this.result = result;
      return this;
    }

    public RouteFilterEnvironment<R> build() {
      return new RouteFilterEnvironment<>(
          router,
          in,
          localConfig,
          defaultPolicy,
          defaultAction,
          localDefaultAction,
          callExprContext,
          callStatementContext,
          result);
    }
  }

  public Builder<R> toBuilder() {
    return new Builder<R>()
        .setRouter(router)
        .setIn(in)
        .setLocalConfig(localConfig)
        .setDefaultPolicy(defaultPolicy)
        .setDefaultAction(defaultAction)
        .setLocalDefaultAction(localDefaultAction)
        .setCallExprContext(callExprContext)
        .setCallStatementContext(callStatementContext)
        .setResult(result);
  }
}
