package datamodel.ipv4;

import com.fasterxml.jackson.annotation.JsonValue;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

public class Ip implements Serializable, Comparable<Ip> {
  private static final long serialVersionUID = -7448270282412559779L;

  private static final HashMap<Long, Ip> CACHE = new HashMap<>();
  public static final Ip AUTO = new Ip(-1L);
  public static final Ip ZERO = new Ip(0L);

  static final int ipBits = 32;

  final long _ip;

  private Ip(long ipLong) {
    _ip = ipLong;
  }

  public static Ip of(@Nullable String ipStr) {
    if (ipStr == null) {
      return null;
    } else {
      long ip = ipStrToLong(ipStr);
      if (!CACHE.containsKey(ip)) {
        CACHE.put(ip, new Ip(ip));
      }
      return CACHE.get(ip);
    }
  }

  public static Ip of(long ip) {
    if (!CACHE.containsKey(ip)) {
      CACHE.put(ip, new Ip(ip));
    }
    return CACHE.get(ip);
  }

  public static Ip parse(String str) {
    if (str == null) {
      return null;
    } else if (str.equals("AUTO/NONE(-1l)")) {
      return AUTO;
    } else {
      return of(str);
    }
  }

  public long asLong() {
    return _ip;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ip ip = (Ip) o;
    return _ip == ip._ip;
  }

  @Override
  public int hashCode() {
    // equals Long.hashCode(_ipLong)
    return (int) (_ip ^ (_ip >>> 32));
  }

  @Override
  @JsonValue
  public String toString() {
    return ipLongToStr(_ip);
  }

  public static boolean valid(long ip) {
    return 0L <= ip && ip <= 0xFFFFFFFFL;
  }

  public static long ipStrToLong(String ipStr) {
    String[] addrArray = ipStr.split("\\.");
    if (addrArray.length != 4) {
      throw new IllegalArgumentException("Invalid IPv4 address: " + ipStr);
    }
    long ipLong = 0;
    try {
      for (int i = 0; i < 4; i++) {
        long segment = Long.parseLong(addrArray[i]);
        checkArgument(
            0 <= segment && segment <= 255,
            "Invalid IPv4 address: %s. %s is an invalid octet",
            ipStr,
            addrArray[i]);
        ipLong = (ipLong << 8) + segment;
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid IPv4 address: " + ipStr, e);
    }
    return ipLong;
  }

  public static String ipLongToStr(long ip) {
    if (!valid(ip)) {
      if (ip == -1L) {
        return "AUTO/NONE(-1l)";
      } else {
        return "INVALID_IP(" + ip + "l)";
      }
    } else {
      return ((ip >> 24) & 0xFF)
          + "."
          + ((ip >> 16) & 0xFF)
          + "."
          + ((ip >> 8) & 0xFF)
          + "."
          + (ip & 0xFF);
    }
  }

  /**
   * @param bin -------- 10000000 00000000 00001010
   * @return prefix{ip:80.0.1.0, mask:255.255.255.0}
   */
  public static HashSet<Prefix> ipBinToPrefixes(byte[] bin) {
    Pair<Integer, Vector<Long>> pair = helper(bin);
    int bound = pair.getKey();
    Vector<Long> ipLongs = pair.getValue();
    HashSet<Prefix> prefixes = new HashSet<>();
    for (long ipLong : ipLongs) {
      prefixes.add(Prefix.of(Ip.of(ipLong), Mask.of(ipBits - bound - 1)));
    }
    return prefixes;
  }

  /**
   * @param bin -------- 10000000 00000000 00001010
   * @return ip: 80.0.1.0 ~ 80.0.1.255
   */
  public static HashSet<Ip> ipBinToIps(byte[] bin) {
    Pair<Integer, Vector<Long>> pair = helper(bin);
    return pair.getValue().stream()
        .flatMap(
            ipLong ->
                IntStream.range(0, (1 << (pair.getKey() + 1))).mapToObj(add -> Ip.of(ipLong + add)))
        .distinct()
        .collect(Collectors.toCollection(HashSet::new));
  }

  private static Pair<Integer, Vector<Long>> helper(byte[] bin) {
    // bound is the right most -1 in bin
    int bound = -1;
    while (bound + 1 < bin.length && bin[bound + 1] == -1) bound++;

    Vector<Long> ipLongs = new Vector<>();
    ipLongs.add(0L);
    int i = bin.length - 1;
    while (i > bound) {
      if (bin[i] != -1) {
        for (int j = 0; j < ipLongs.size(); j++) {
          ipLongs.set(j, (ipLongs.get(j) << 1) + bin[i]);
        }
      } else {
        Vector<Long> tmp = new Vector<>(ipLongs.size() * 2);
        for (int j = 0; j < ipLongs.size(); j++) {
          long ipLong = ipLongs.get(j) << 1;
          tmp.add(2 * j, ipLong);
          tmp.add(2 * j + 1, ipLong + 1);
        }
        ipLongs = tmp;
      }
      i--;
    }
    for (int j = 0; j < ipLongs.size(); j++) {
      ipLongs.set(j, ipLongs.get(j) << (bound + 1));
    }
    return new Pair<>(bound, ipLongs);
  }

  @Override
  public int compareTo(@NotNull Ip o) {
    return Long.compare(_ip, o._ip);
  }
}
