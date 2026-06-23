package datamodel.collector;

import datamodel.aspath.AsPathRegex;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;

import java.util.Collections;
import java.util.Set;

public interface Collector {
  default Set<PrefixRange> collectPrefixRanges() {
    return Collections.emptySet();
  }

  default Set<CommunityRegex> collectCommunityRegexes() {
    return Collections.emptySet();
  }

  default Set<AsPathRegex> collectAsPathRegexes() {
    return Collections.emptySet();
  }
}
