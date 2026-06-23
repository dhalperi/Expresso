package datamodel.ipv4;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/** An IPv4 Prefix */
public final class Prefix implements Comparable<Prefix>, Serializable {
  private static final long serialVersionUID = -3107005764886552265L;

  private static final Multimap<Integer, Prefix> CACHE = HashMultimap.create();
  public static Prefix ZERO = new Prefix(Mask.of(0), 0);

  final Ip _ip;
  final Mask _mask;
  final long _long;

  private Prefix(Mask mask, long l) {
    _mask = mask;
    _long = l;
    _ip = Ip.of(_long);
  }

  public static Prefix of(Ip ip, Mask mask) {
    long l = ip._ip & mask._ip._ip;
    int hash = Objects.hash(mask, l);
    if (CACHE.containsKey(hash)) {
      for (Prefix prefix : CACHE.get(hash)) {
        if (prefix._long == l && prefix._mask._len == mask._len) {
          return prefix;
        }
      }
    }
    Prefix prefix = new Prefix(mask, l);
    CACHE.put(hash, prefix);
    return prefix;
  }

  public static Prefix of(String str) {
    String[] tmp = str.split("/");
    Ip ip = Ip.of(tmp[0]);
    Mask mask = Mask.of(32);
    if (tmp.length == 2) {
      if (tmp[1].contains(".")) {
        mask = Mask.of(tmp[1]);
      } else {
        mask = Mask.of(Integer.parseInt(tmp[1]));
      }
    }
    return Prefix.of(ip, mask);
  }

  public Ip getIp() {
    return _ip;
  }

  public long getLong() {
    return _long;
  }

  public int getPreLength() {
    return _mask._len;
  }

  public boolean prefixMatchStrict(Prefix another) {
    return _ip.equals(another._ip) && _mask.equals(another._mask);
  }

  public boolean prefixMatchLoose(Prefix another) {
    if (_mask._len > another._mask._len) return false;
    else return ipMatch(another._ip);
  }

  public boolean ipMatch(Ip ip) {
    return _long == (ip._ip & _mask._ip._ip);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Prefix prefix = (Prefix) o;
    return _long == prefix._long && _mask.equals(prefix._mask);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_mask, _long);
  }

  @Override @JsonValue
  public String toString() {
    return _ip.toString() + "/" + _mask._len;
  }

  @Override
  public int compareTo(@NotNull Prefix another) {
    // since TreeMap is in increasing order by default,
    // we have to return -1 if this.preLength > another.preLength
    return Comparator.comparing(Prefix::getPreLength, Comparator.reverseOrder())
        .thenComparing(p -> p._long)
        .compare(this, another);
  }
}
