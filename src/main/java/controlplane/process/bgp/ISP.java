package controlplane.process.bgp;

import com.google.common.base.MoreObjects;
import controlplane.rib.Rib;
import controlplane.route.BgpRoute;
import datamodel.ipv4.Ip;
import datamodel.path.PathHop;

import java.util.Objects;

public class ISP implements PathHop {
    final Ip ip;
    final long as;
    final String neighbors;
    final Rib<BgpRoute> rib;

    public ISP(Ip ip, long as, String neighbors) {
        this.ip = ip;
        this.as = as;
        this.neighbors = neighbors;
        this.rib = new Rib<>();
    }

    public Ip getIp() {
        return ip;
    }

    public long getAs() {
        return as;
    }

    public String getName() {
        return String.format("ISP(%s, %s, %s)", neighbors, as, ip);
    }

    public Rib<BgpRoute> getRib() {
        return rib;
    }

    @Override
    public String hopString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ISP isp = (ISP) o;
        return Objects.equals(ip, isp.ip) && as == isp.as;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, as);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ip", ip)
                .add("as", as)
                .add("rib", rib)
                .toString();
    }
}
