package datamodel.ipv4;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * An {@link IpAddress} is different from a {@link Prefix} although they both have an {@link Ip} and a {@link Mask} as
 * member objects.
 *
 * E.g.,
 * Prefix("192.168.0.1", 24).ip = 192.168.0.0
 * IpAddress("192.168.0.1", 24).ip = 192.168.0.1
 */
public class IpAddress implements Serializable, Comparable<IpAddress>{
    private static final long serialVersionUID = -2393750836753502604L;

    private final Ip _ip;
    private final Mask _mask;

    public IpAddress(Ip ip, Mask mask) {
        _ip = ip;
        _mask = mask;
    }

    public Ip getIp() {
        return _ip;
    }

    public Mask getMask() {
        return _mask;
    }

    public int getNetworkBits() {
        return _mask._len;
    }

    /**
     * @param str "192.168.0.1/32" like string
     * @return an {@link IpAddress} object
     */
    public static IpAddress parse(String str) {
        String[] tmp = str.split("/");
        Ip ip = Ip.of(tmp[0]);
        Mask mask = Mask.of(Integer.parseInt(tmp[1]));
        return new IpAddress(ip, mask);
    }

    public Prefix toPrefix() {
        return Prefix.of(_ip, _mask);
    }

    public Prefix toPrefix32() {
        return Prefix.of(_ip, Mask.of(32));
    }

    @Override
    public int compareTo(@NotNull IpAddress o) {
        return Comparator
                .comparing(IpAddress::getIp)
                .thenComparing(IpAddress::getMask)
                .compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpAddress ipAddress = (IpAddress) o;
        return Objects.equals(_ip, ipAddress._ip) &&
               Objects.equals(_mask, ipAddress._mask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_ip, _mask);
    }

    @Override
    public String toString() {
        return _ip.toString() + '/' + _mask._len;
    }
}
