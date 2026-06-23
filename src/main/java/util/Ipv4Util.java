package util;

import datamodel.Range;
import datamodel.ipv4.IpAddress;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;

public class Ipv4Util {
  public static PrefixRange getPrefixRange(Prefix prefix) {
    return PrefixRange.of(prefix, new Range<>(prefix.getPreLength()));
  }

  public static PrefixRange getPrefixRange(IpAddress ipAddress) {
    return PrefixRange.of(ipAddress.toPrefix(), new Range<>(ipAddress.getNetworkBits()));
  }

  public static PrefixRange getPrefixRange32(IpAddress ipAddress) {
    return PrefixRange.of(ipAddress.toPrefix(), new Range<>(32));
  }
}
