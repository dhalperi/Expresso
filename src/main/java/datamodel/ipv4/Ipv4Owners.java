package datamodel.ipv4;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import datamodel.Range;
import datamodel.filterlist.FilterList;
import org.jetbrains.annotations.NotNull;
import util.MathUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Ipv4Owners {
  private static class BitTrieNode {
    final long ip;
    final int length;
    BitTrieNode left;
    BitTrieNode right;
    SortedSet<Owner> ipOwners;
    SortedSet<Owner> prefixOwners;
    SortedSetMultimap<Range<Integer>, Owner> prefixRangeOwners;

    public BitTrieNode(long ip, int length) {
      this.ip = ip;
      this.length = length;
      this.ipOwners = new TreeSet<>();
      this.prefixOwners = new TreeSet<>();
      this.prefixRangeOwners = TreeMultimap.create();
    }

    public void makeLeft() {
      left = new BitTrieNode(ip << 1, length + 1);
    }

    public void makeRight() {
      right = new BitTrieNode((ip << 1) + 1, length + 1);
    }

    public boolean containsRange(Range<Integer> range) {
      return prefixRangeOwners.keySet().stream().anyMatch(r -> r.contains(range));
    }

    public boolean isEmpty() {
      return ipOwners.isEmpty() && prefixOwners.isEmpty() && prefixRangeOwners.isEmpty();
    }

    public Ip getIp() {
      return Ip.of(ip << (32 - length));
    }

    public Prefix getPrefix() {
      return Prefix.of(getIp(), Mask.of(length));
    }
  }

  private static class Owner implements Comparable<Owner> {
    String owner;

    public Owner(String str) {
      owner = str;
    }

    public Owner(Interface intf) {
      owner =
          intf.getInterfaceName().getFullName()
              + ": "
              + Objects.requireNonNull(intf.getPrimaryIpAddress()).getIp().toString();
    }

    public Owner(VirtualRouter vr, String reason) {
      owner = vr.getFullName() + ": " + reason;
    }

    public Owner(Router r, FilterList fl) {
      owner = r.getRouterName() + ": " + fl.getName();
    }

    @Override
    public int compareTo(@NotNull Ipv4Owners.Owner o) {
      return owner.compareTo(o.owner);
    }
  }

  private final BitTrieNode root;

  public Ipv4Owners() {
    root = new BitTrieNode(0, 0);
  }

  public void add(Ip ip, Interface intf) {
    byte[] bv = calBinRep(ip);
    Collection<BitTrieNode> nodes = findLeafNodes(bv);
    nodes.forEach(node -> node.ipOwners.add(new Owner(intf)));
  }

  public void addInternalPrefix(Prefix prefix) {
    add(prefix, "internalPrefix");
  }

  public void addInternalPrefix(IpAddress ipAddress) {
    add(ipAddress.toPrefix(), "internalPrefix: " + ipAddress.getIp().toString());
  }

  public void add(Prefix prefix, VirtualRouter vr, String reason) {
    add(prefix, vr.getFullName() + ": " + reason);
  }

  private void add(Prefix prefix, String reason) {
    byte[] bv = calBinRep(prefix);
    Collection<BitTrieNode> nodes = findLeafNodes(bv);
    Owner owner = new Owner(reason);
    nodes.forEach(node -> node.prefixOwners.add(owner));
    nodes.forEach(node -> node.prefixRangeOwners.put(new Range<>(prefix.getPreLength()), owner));
  }

  public void add(PrefixRange prefixRange, Router r, FilterList filterList) {
    byte[] bv = calBinRep(prefixRange);
    Collection<BitTrieNode> nodes = findLeafNodes(bv);
    nodes.forEach(
        node -> node.prefixRangeOwners.put(prefixRange.getRange(), new Owner(r, filterList)));
  }

  private Collection<BitTrieNode> findLeafNodes(byte[] bv) {
    Deque<BitTrieNode> queue = new LinkedList<>();
    queue.add(root);
    for (int idx = bv.length - 1; idx >= 0; idx--) {
      byte b = bv[idx];
      int s = queue.size();
      for (int i = 0; i < s; i++) {
        BitTrieNode curNode = queue.pop();
        if (b != 0) {
          if (curNode.right == null) curNode.makeRight();
          queue.add(curNode.right);
        }
        if (b != 1) {
          if (curNode.left == null) curNode.makeLeft();
          queue.add(curNode.left);
        }
      }
    }
    return queue;
  }

  private List<BitTrieNode> findNodes(byte[] bv) {
    LinkedList<BitTrieNode> list = new LinkedList<>();
    list.add(root);
    for (int idx = bv.length - 1; idx >= 0; idx--) {
      byte b = bv[idx];
      BitTrieNode curNode = list.get(0);
      if (b == 0) {
        if (curNode.left == null) curNode.makeLeft();
        list.addFirst(curNode.left);
      } else if (b == 1) {
        if (curNode.right == null) curNode.makeRight();
        list.addFirst(curNode.right);
      } else {
        throw new IllegalArgumentException();
      }
    }
    return list.stream().filter(node -> !node.isEmpty()).collect(Collectors.toList());
  }

  public SortedMap<String, SortedMap<String, SortedSet<String>>> findOwners(Ip ip) {
    byte[] bv = calBinRep(ip);
    List<BitTrieNode> nodes = findNodes(bv);
    return organizeOwners(nodes);
  }

  public SortedMap<String, SortedMap<String, SortedSet<String>>> findOwners(Prefix prefix) {
    byte[] bv = calBinRep(prefix);
    List<BitTrieNode> nodes = findNodes(bv);
    return organizeOwners(nodes);
  }

  public SortedMap<String, SortedMap<String, SortedSet<String>>> findExactOwners(Prefix prefix) {
    byte[] bv = calBinRep(prefix);
    List<BitTrieNode> nodes =
        findNodes(bv).stream()
            .filter(node -> node.length == prefix.getPreLength())
            .collect(Collectors.toList());
    return organizeOwners(nodes);
  }

  public SortedMap<String, SortedMap<String, SortedSet<String>>> findOwners(
      PrefixRange prefixRange) {
    byte[] bv = calBinRep(prefixRange);
    List<BitTrieNode> nodes = findNodes(bv);
    nodes =
        nodes.stream()
            .filter(node -> node.containsRange(prefixRange.getRange()))
            .collect(Collectors.toList());
    return organizeOwners(nodes);
  }

  private SortedMap<String, SortedMap<String, SortedSet<String>>> organizeOwners(
      List<BitTrieNode> nodes) {
    SortedMap<String, SortedMap<String, SortedSet<String>>> owners = new TreeMap<>();
    nodes.forEach(
        node -> {
          String prefix = node.getPrefix().toString();
          SortedMap<String, SortedSet<String>> map = new TreeMap<>();
          if (!node.ipOwners.isEmpty()) {
            map.put(
                "ip",
                node.ipOwners.stream()
                    .map(o -> o.owner)
                    .collect(Collectors.toCollection(TreeSet::new)));
          }
          if (!node.prefixOwners.isEmpty()) {
            map.put(
                "prefix",
                node.prefixOwners.stream()
                    .map(o -> o.owner)
                    .collect(Collectors.toCollection(TreeSet::new)));
          }
          if (!node.prefixRangeOwners.isEmpty()) {
            map.put(
                "prefixRange",
                node.prefixRangeOwners.values().stream()
                    .map(o -> o.owner)
                    .collect(Collectors.toCollection(TreeSet::new)));
          }
          if (!node.isEmpty()) owners.put(prefix, map);
        });
    return owners;
  }

  private SortedMap<String, SortedMap<String, SortedSet<String>>> organizeOwners(
      List<BitTrieNode> nodes, Range<Integer> range) {
    SortedMap<String, SortedMap<String, SortedSet<String>>> owners = new TreeMap<>();
    nodes.stream()
        .filter(node -> !node.prefixRangeOwners.isEmpty())
        .forEach(
            node -> {
              String ip = Ip.of(node.ip << (32 - node.length)).toString();
              SortedMap<String, SortedSet<String>> map = new TreeMap<>();
              map.put(
                  "prefixRange",
                  node.prefixRangeOwners.asMap().entrySet().stream()
                      .filter(e -> e.getKey().contains(range))
                      .flatMap(e -> e.getValue().stream().map(o -> o.owner))
                      .collect(Collectors.toCollection(TreeSet::new)));
              owners.put(ip, map);
            });
    return owners;
  }

  private List<BitTrieNode> collectAllNodes() {
    List<BitTrieNode> allNodes = new LinkedList<>();
    allNodes.add(root);
    int l = 0, h;
    for (int i = 0; i < 32; i++) {
      h = allNodes.size();
      for (int j = l; j < h; j++) {
        BitTrieNode curNode = allNodes.get(j);
        if (curNode.left != null) allNodes.add(curNode.left);
        if (curNode.right != null) allNodes.add(curNode.right);
      }
      l = h;
    }
    return allNodes;
  }

  public SortedMap<String, SortedMap<String, SortedSet<String>>> collectOwners() {
    List<BitTrieNode> allNodes = collectAllNodes();
    return organizeOwners(allNodes);
  }

  private static byte[] calBinRep(Ip ip) {
    return MathUtil.calBinRep(ip.asLong(), 32);
  }

  private static byte[] calBinRep(Prefix prefix) {
    byte[] bv = MathUtil.calBinRep(prefix.getLong(), 32);
    return Arrays.copyOfRange(bv, 32 - prefix.getPreLength(), 32);
  }

  private static byte[] calBinRep(PrefixRange prefixRange) {
    return calBinRep(prefixRange.getPrefix());
  }
}
