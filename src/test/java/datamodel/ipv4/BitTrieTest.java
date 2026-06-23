package datamodel.ipv4;

import com.google.common.collect.ImmutableList;
import datamodel.Range;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class BitTrieTest {
  @Test
  public void testBitTrie() {
    byte[] bits = new byte[32];
    Arrays.fill(bits, (byte) -1);
    bits[0] = 1;
    bits[2] = 0;

    BitTrie bitTrie = new BitTrie();
    bitTrie.add(bits, ImmutableList.of(new Range<>(30), new Range<>(32)));
    Collection<PrefixRange> prefixRanges = bitTrie.collectPrefixRanges();

    prefixRanges.forEach(System.out::println);
  }

  @Test
  public void testBitTrieReduce() {
    byte[] bits = new byte[32];
    Arrays.fill(bits, (byte) -1);

    BitTrie bitTrie = new BitTrie();

    // 128.0.0.0/2:2~32
    bits[0] = 1;
    bits[1] = 0;
    bitTrie.add(bits, ImmutableList.of(new Range<>(2, 32)));

    // 192.0.0.0/2:2~32
    bits[0] = 1;
    bits[1] = 1;
    bitTrie.add(bits, ImmutableList.of(new Range<>(2, 32)));

    Collection<PrefixRange> prefixRanges = bitTrie.collectPrefixRanges();

    prefixRanges.forEach(System.out::println);
  }
}
