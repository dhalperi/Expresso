package datamodel.ipv4;

import datamodel.Range;
import util.MathUtil;

import java.util.*;

public class BitTrie {
  protected BitTrieNode root;

  public BitTrie() {
    root = new BitTrieNode(0L, 0);
  }

  public void add(byte[] bits, Collection<Range<Integer>> ranges) {
    if (bits.length > 32) {
      throw new IllegalArgumentException();
    }
    int limit = bits.length;
    while (bits[limit - 1] == -1) limit--;
    traverse(bits, 0, limit, ranges, root);
  }

  private void traverse(
      byte[] bits, int idx, int limit, Collection<Range<Integer>> ranges, BitTrieNode curNode) {
    if (idx >= limit) {
      curNode.addRanges(ranges);
    } else {
      if (bits[idx] != 1) {
        traverse(bits, idx + 1, limit, ranges, curNode.getLeft());
      }
      if (bits[idx] != 0) {
        traverse(bits, idx + 1, limit, ranges, curNode.getRight());
      }
    }
  }

  public Collection<PrefixRange> collectPrefixRanges() {
    root.reduce();
    return root.collectPrefixRanges();
  }

  private static class BitTrieNode {
    private final long ip;
    private final int length;
    private BitTrieNode left;
    private BitTrieNode right;
    private Set<Range<Integer>> ranges;

    public BitTrieNode(long ip, int length) {
      this.ip = ip;
      this.length = length;
      this.ranges = new TreeSet<>();
    }

    public BitTrieNode getLeft() {
      if (left == null) {
        left = new BitTrieNode(ip << 1, length + 1);
      }
      return left;
    }

    public BitTrieNode getRight() {
      if (right == null) {
        right = new BitTrieNode((ip << 1) + 1, length + 1);
      }
      return right;
    }

    public void addRanges(Collection<Range<Integer>> newRanges) {
      ranges.addAll(newRanges);
      ranges = new TreeSet<>(MathUtil.combineRanges(ranges));
    }

    public Collection<PrefixRange> collectPrefixRanges() {
      SortedSet<PrefixRange> prefixRanges = new TreeSet<>();

      Prefix prefix = Prefix.of(Ip.of(ip << (32 - length)), Mask.of(length));
      ranges.forEach(range -> prefixRanges.add(PrefixRange.of(prefix, range)));
      if (left != null) {
        prefixRanges.addAll(left.collectPrefixRanges());
      }
      if (right != null) {
        prefixRanges.addAll(right.collectPrefixRanges());
      }

      return prefixRanges;
    }

    public void reduce() {
      if (left != null) {
        left.reduce();
      }
      if (right != null) {
        right.reduce();
      }
      if (left != null && right != null) {
        if (left.fullRange() && right.fullRange()) {
          left = null;
          right = null;
          addRanges(Collections.singleton(new Range<>(length + 1, 32)));
        }
      }
    }

    public boolean fullRange() {
      return ranges.size() == 1 && ranges.iterator().next().equals(new Range<>(length, 32));
    }
  }
}
