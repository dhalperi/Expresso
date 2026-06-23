package dataplane.analysis;

import bdd.BddEnvcWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import datamodel.ipv4.Prefix;
import datamodel.path.Path;

import java.util.Objects;
import java.util.SortedSet;

import static main.Controller.bddManager;

public class ReachabilityNode {
  private static final String PROP_PATH = "path";
  private static final String PROP_PREFIXES = "prefixes";
  private static final String PROP_TYPE = "type";

  public enum Type {
    LOOP,
    BLACKHOLE,
    ARRIVE_LOOPBACK,
    ARRIVE_NULL0,
    ARRIVE,
    EXIT,
    // helper type
    HIJACKED,
    UNREACHABLE
  }

  final Path path;
  int pkt;
  SortedSet<Prefix> prefixes;
  final Type type;

  ReachabilityNode(Path path, int pkt, Type type) {
    this.path = path;
    this.pkt = pkt;
    this.type = type;
  }

  private ReachabilityNode(Path path, SortedSet<Prefix> prefixes, Type type) {
    this.path = path;
    this.prefixes = prefixes;
    this.type = type;
  }

  private static int packet(int pkt) {
    if (DPAnalyzer.apvMethod) return pkt;

    int packet = pkt;
    for (BddEnvcWrapper.IspVarWrapper var : bddManager.getBddEnvcWrapper().getIspVars().values()) {
      packet = bddManager.and1(packet, var.getCpDpRelation(bddManager.getBDD()));
      if (packet == 0) break;
    }
    return packet;
  }

  /** We will copy the path */
  public static ReachabilityNode hijacked(Path path, int pkt) {
    //        return new ReachabilityNode(new Path(path), pkt, Type.LEAKED);
    return new ReachabilityNode(new Path(path), packet(pkt), Type.HIJACKED);
  }

  public static ReachabilityNode unreachable(Path path, int pkt) {
    //        return new ReachabilityNode(new Path(path), pkt, Type.UNREACHABLE);
    return new ReachabilityNode(new Path(path), packet(pkt), Type.UNREACHABLE);
  }

  public static ReachabilityNode loop(Path path, SortedSet<Prefix> prefixes) {
    return new ReachabilityNode(new Path(path), prefixes, Type.LOOP);
  }

  @JsonProperty(PROP_PATH)
  public Path getPath() {
    return path;
  }

  public int getPkt() {
    return pkt;
  }

  @JsonProperty(PROP_TYPE)
  public Type getType() {
    return type;
  }

  @JsonProperty(PROP_PREFIXES)
  public SortedSet<Prefix> getPrefixes() {
    if (prefixes == null) {
      prefixes = bddManager.getBddPrefixWrapper().extractPrefixesFromPacket(pkt);
    }
    return prefixes;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("prefixes", getPrefixes())
        .add("path", path)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReachabilityNode that = (ReachabilityNode) o;
    return pkt == that.pkt && Objects.equals(path, that.path) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, pkt, type);
  }
}
