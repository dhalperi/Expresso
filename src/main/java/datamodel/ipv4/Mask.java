package datamodel.ipv4;

import org.jetbrains.annotations.NotNull;
import util.MathUtil;

import java.io.Serializable;

public class Mask implements Serializable, Comparable<Mask> {
    private static final long serialVersionUID = -5853640926214360491L;

    private static final Mask[] MASK = new Mask[33];

    final Ip _ip;
    final int _len;

    private Mask(Ip ip, int len) {
        _ip = ip;
        _len = len;
    }

    public static Mask of(Ip ip) {
        int len = maskIpToLen(ip);
        if (MASK[len] == null) {
            MASK[len] = new Mask(ip, len);
        }
        return MASK[len];
    }

    public static Mask of(int len) {
        if (len < 0 || len > 32) {
            throw new IllegalArgumentException("IPv4 mask cannot be less than 0 or greater than 32");
        }
        if (MASK[len] == null) {
            Ip ip = maskLenToIp(len);
            MASK[len] = new Mask(ip, len);
        }
        return MASK[len];
    }

    public static Mask of(String str) {
        return of(Ip.of(str));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mask mask = (Mask) o;
        return _len == mask._len;
    }

    @Override
    public int hashCode() {
        return _len;
    }

    public static int maskIpToLen(Ip ip) {
        long tmp1 = ip._ip & (-ip._ip);
//        if (tmp1 == 0) return 0;
        int l = 0, h = 32;
        while (l <= h) {
            int mid = (l + h) >> 1;
            long tmp2 = 1L << mid;
            if (tmp1 == tmp2) {
                return Ip.ipBits - mid;
            }
            else if (tmp1 > tmp2) {
                l = mid + 1;
            }
            else {
                h = mid - 1;
            }
        }
        return 0;
    }

    public static Ip maskLenToIp(int len) {
        long ipLong = 0;
        for (int i = 24; i >= 0; i -= 8) {
            if (len > i) {
                ipLong += (256 - MathUtil.power2(8 + i - len)) << (24 - i);
                len = i;
            }
        }
        return Ip.of(ipLong);
    }

    @Override
    public int compareTo(@NotNull Mask o) {
        return Integer.compare(_len, o._len);
    }
}
