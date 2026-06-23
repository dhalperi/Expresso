package controlplane.process.ospf;

import controlplane.network.Interface;

import java.util.List;

public class OspfArea {
    long name;
    boolean injectDefaultRoute;
    List<Interface> interfaces;
    int metricOfDefaultRoute;

    public OspfArea(long name, boolean injectDefaultRoute, List<Interface> interfaces, int metricOfDefaultRoute) {
        this.name = name;
        this.injectDefaultRoute = injectDefaultRoute;
        this.interfaces = interfaces;
        this.metricOfDefaultRoute = metricOfDefaultRoute;
    }

    public long getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        long name;
        boolean injectDefaultRoute;
        List<Interface> interfaces;
        int metricOfDefaultRoute;

        private Builder() {

        }

        public OspfArea build() {
            return new OspfArea(
                    name,
                    injectDefaultRoute,
                    interfaces,
                    metricOfDefaultRoute
            );
        }

        public Builder setName(long name) {
            this.name = name;
            return this;
        }

        public Builder setInjectDefaultRoute(boolean injectDefaultRoute) {
            this.injectDefaultRoute = injectDefaultRoute;
            return this;
        }

        public Builder setInterfaces(List<Interface> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        public Builder setMetricOfDefaultRoute(int metricOfDefaultRoute) {
            this.metricOfDefaultRoute = metricOfDefaultRoute;
            return this;
        }
    }
}
