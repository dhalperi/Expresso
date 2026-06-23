package controlplane.process;

import controlplane.network.VirtualRouter;
import controlplane.route.RoutingProtocol;
import datamodel.routepolicy.RoutePolicy;

public class RedistributionConfig {
    final VirtualRouter vrf;
    final RoutingProtocol protocol;

    // for rip, isis, ospf
    final Integer processId;

    // for bgp
    final Long med;

    // for ospf
    final Integer limit;
    final Boolean permitIbgp;
    final Long cost;
    final Integer type;
    final Long tag;

    final RoutePolicy routePolicy;

    private RedistributionConfig(
            VirtualRouter vrf,
            RoutingProtocol protocol,
            Integer processId,
            Long med,
            Integer limit,
            Boolean permitIbgp,
            Long cost,
            Integer type,
            Long tag,
            RoutePolicy routePolicy) {
        this.vrf = vrf;
        this.protocol = protocol;
        this.processId = processId;
        this.med = med;
        this.limit = limit;
        this.permitIbgp = permitIbgp;
        this.cost = cost;
        this.type = type;
        this.tag = tag;
        this.routePolicy = routePolicy;
    }

    public VirtualRouter getVrf() {
        return vrf;
    }

    public RoutingProtocol getProtocol() {
        return protocol;
    }

    public Integer getProcessId() {
        return processId;
    }

    public Long getMed() {
        return med;
    }

    public Integer getLimit() {
        return limit;
    }

    public Boolean getPermitIbgp() {
        return permitIbgp;
    }

    public Long getCost() {
        return cost;
    }

    public Integer getType() {
        return type;
    }

    public Long getTag() {
        return tag;
    }

    public RoutePolicy getRoutePolicy() {
        return routePolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        VirtualRouter vrf;
        RoutingProtocol protocol;
        Integer processId;
        Long med;
        Integer limit;
        Boolean permitIbgp;
        Long cost;
        Integer type;
        Long tag;
        RoutePolicy routePolicy;

        private Builder() {}

        public RedistributionConfig build() {
            return new RedistributionConfig(
                    vrf,
                    protocol,
                    processId,
                    med,
                    limit,
                    permitIbgp,
                    cost,
                    type,
                    tag,
                    routePolicy
            );
        }

        public Builder setVrf(VirtualRouter vrf) {
            this.vrf = vrf;
            return this;
        }

        public Builder setProtocol(RoutingProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setProcessId(Integer processId) {
            this.processId = processId;
            return this;
        }

        public Builder setMed(Long med) {
            this.med = med;
            return this;
        }

        public Builder setLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder setPermitIbgp(Boolean permitIbgp) {
            this.permitIbgp = permitIbgp;
            return this;
        }

        public Builder setCost(Long cost) {
            this.cost = cost;
            return this;
        }

        public Builder setType(Integer type) {
            this.type = type;
            return this;
        }

        public Builder setTag(Long tag) {
            this.tag = tag;
            return this;
        }

        public Builder setRoutePolicy(RoutePolicy routePolicy) {
            this.routePolicy = routePolicy;
            return this;
        }
    }
}
