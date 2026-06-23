package dataplane.analysis;

import atomic.bdd.BddAtomicPredicates;
import atomic.bdd.BddRepresentedImpl;
import bdd.BddEnvcWrapper;
import controlplane.network.Interface;
import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import dataplane.fib.Fib;
import dataplane.fib.FibEntry;
import main.Configuration;
import main.ExpressoLogger;
import util.BddUtil;
import util.TimeUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static main.Controller.bddManager;
import static main.Controller.cpExecutor;

public class DPAnalyzer {
  public ReachabilityDB reachabilityDB;

  public void analyzeDataPlane() {
    ExpressoLogger.pushContext("DPA");
    TimeUtil timer = new TimeUtil();
    timer.begin();

    computeFwdBDD();

    buildReachabilityTree();

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();
  }

  static boolean apvMethod = false;

  public void setApvMethod(boolean apv) {
    apvMethod = apv;
  }

  public boolean isApvMethod() {
    return apvMethod;
  }

  void computeFwdBDD() {
    ExpressoLogger.pushContext("FWD " + (apvMethod ? "apv" : "apk"));

    TimeUtil timer = new TimeUtil();
    timer.begin();
    for (Router router : cpExecutor.l3Topology.getNodes().values()) {
      for (VirtualRouter vrf : router.getVirtualRouters().values()) {
        if (apvMethod) {
          computeFwdBddApv(vrf);
        } else {
          computeFwdBddApk(vrf);
        }
        //        postProcessFwdBdd();
      }
    }
    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();
  }

  HashMap<VirtualRouter, HashMap<Interface, Integer>> intfPredicates = new HashMap<>();
  HashMap<VirtualRouter, Integer> vrfPredicates = new HashMap<>();

  void computeFwdBddApv(VirtualRouter vrf) {
    HashMap<Interface, List<Integer>> fwdBDDs = new HashMap<>();

    Fib fib = vrf.getFib();
    int matched = 0;
    int intersect;
    for (int length = 32; length >= 0; length--) {
      if (matched == 1) {
        ExpressoLogger.log(
            ExpressoLogger.LEVEL.INFO, String.format("%s stopping at prefix length %d", vrf, length));
        break;
      }
      int notMatched = bddManager.not1(matched);
      int lengthBdd = bddManager.getBddPrefixWrapper().encodeLength(length);
      List<Integer> list = new LinkedList<>();
      list.add(matched);
      for (FibEntry fibEntry : fib.getEntries()) {
        intersect = bddManager.and1(lengthBdd, fibEntry.getPrefixesBdd());
        if (intersect == 0) continue;
        intersect = bddManager.getBddPrefixWrapper().eraseLength(intersect, length);
        intersect = bddManager.getBddEnvcWrapper().transformCpEnvc2DpEnvc(intersect, length);
        intersect = bddManager.and1(intersect, notMatched);
        if (intersect != 0) {
          Interface intf = fibEntry.getNextHopInterface();
          fwdBDDs.computeIfAbsent(intf, inf -> new LinkedList<>()).add(intersect);
          list.add(intersect);
        }
      }
      matched = BddUtil.orInBatch(bddManager.getBDD(), list);
    }
    HashMap<Interface, Integer> tmp =
        new HashMap<>(
            fwdBDDs.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> BddUtil.orInBatchReverse(bddManager.getBDD(), e.getValue()))));
    intfPredicates.put(vrf, tmp);
    vrfPredicates.put(vrf, BddUtil.orInBatch(bddManager.getBDD(), tmp.values()));
  }

  void computeFwdBddApk(VirtualRouter vrf) {
    HashMap<Interface, Integer> matchesWithLength = new HashMap<>();
    Fib fib = vrf.getFib();
    int oldP, newP;
    for (FibEntry fibEntry : fib.getEntries()) {
      Interface intf = fibEntry.getNextHopInterface();
      oldP = matchesWithLength.getOrDefault(intf, 0);
      newP = bddManager.or1(oldP, fibEntry.getPrefixesBdd());
      matchesWithLength.put(intf, newP);
    }

    HashMap<Interface, Integer> matchesWithoutLength =
        new HashMap<>(
            matchesWithLength.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> bddManager.getBddPrefixWrapper().eraseLength(e.getValue()))));

    HashMap<Interface, Integer> matchesOnlyHeaders =
        new HashMap<>(
            matchesWithoutLength.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> bddManager.getBddPrefixWrapper().eraseEnvcUsingExists(e.getValue()))));

    Set<BddRepresentedImpl<Interface>> origins =
        matchesOnlyHeaders.entrySet().stream()
            .map(e -> new BddRepresentedImpl<>(e.getKey(), e.getValue()))
            .collect(Collectors.toSet());

    BddAtomicPredicates<BddRepresentedImpl<Interface>> aps = new BddAtomicPredicates<>(origins);
    Map<Integer, Set<BddRepresentedImpl<Interface>>> atomToOrigin = aps.getAtomToOrigins();

    HashMap<Interface, Integer> fwdBdds = new HashMap<>();
    if (atomToOrigin.values().stream().anyMatch(s -> s.size() == 1)) {
      ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, vrf + ": port predicates not overlapping");
      fwdBdds = matchesWithoutLength;
    } else {
      for (Map.Entry<Integer, Set<BddRepresentedImpl<Interface>>> entry : atomToOrigin.entrySet()) {
        int atom = entry.getKey();
        if (entry.getValue().size() == 1) {
          Interface intf = entry.getValue().iterator().next().getOwner();
          int matchWithoutLength = matchesWithoutLength.get(intf);
          oldP = fwdBdds.getOrDefault(intf, 0);
          newP = bddManager.or1(bddManager.and1(atom, matchWithoutLength), oldP);
          fwdBdds.put(intf, newP);
          ExpressoLogger.log(
              ExpressoLogger.LEVEL.INFO,
              String.format("%s: atom %d belongs to only one port %s", vrf, entry.getKey(), intf));
        } else if (entry.getValue().size() > 1) {
          ExpressoLogger.log(
              ExpressoLogger.LEVEL.INFO,
              String.format(
                  "%s: worst case, enumerating for atom %d from 32 to 0", vrf, entry.getKey()));
          int matched = 0;
          for (int len = 32; len >= 0; len--) {
            List<Integer> list = new LinkedList<>();
            list.add(matched);
            int pfxBdd = bddManager.and1(atom, bddManager.getBddPrefixWrapper().encodeLength(len));
            for (BddRepresentedImpl<Interface> origin : entry.getValue()) {
              Interface intf = origin.getOwner();
              int matchWithLength = matchesWithLength.get(intf);
              int tmp1 = bddManager.and1(pfxBdd, matchWithLength);
              if (tmp1 != 0) {
                tmp1 = bddManager.getBddEnvcWrapper().transformCpEnvc2DpEnvc(tmp1, len);
                tmp1 = bddManager.minus1(tmp1, matched);
                oldP = fwdBdds.getOrDefault(intf, 0);
                newP = bddManager.or1(tmp1, oldP);
                fwdBdds.put(intf, newP);
              }
            }
            matched = BddUtil.orInBatch(bddManager.getBDD(), list);
          }
        }
      }
    }
    intfPredicates.put(vrf, fwdBdds);
    vrfPredicates.put(vrf, BddUtil.orInBatch(bddManager.getBDD(), fwdBdds.values()));
  }

  void postProcessFwdBdd() {
    if (!apvMethod) {
      int tmp =
          BddUtil.andInBatch(
              bddManager.getBDD(),
              bddManager.getBddEnvcWrapper().getIspVars().values().stream()
                  .map(wrapper -> wrapper.getCpDpRelation(bddManager.getBDD()))
                  .collect(Collectors.toSet()));
      intfPredicates
          .values()
          .forEach(
              map -> map.entrySet().forEach(e -> e.setValue(bddManager.and(e.getValue(), tmp))));
      vrfPredicates.entrySet().forEach(e -> e.setValue(bddManager.and(e.getValue(), tmp)));
    }
  }

  void buildReachabilityTree() {
    ExpressoLogger.pushContext("REACH TREE");

    if (Configuration.ENABLE_ACL || Configuration.ENABLE_TRAFFIC_POLICY) {
      String msg =
          Stream.of(
                  Configuration.ENABLE_ACL ? "acl enabled" : null,
                  Configuration.ENABLE_TRAFFIC_POLICY ? "traffic policy enabled" : null)
              .filter(Objects::nonNull)
              .collect(Collectors.joining(", "));
      ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, msg);
    }

    TimeUtil timer = new TimeUtil();
    timer.begin();

    reachabilityDB = new ReachabilityDB(intfPredicates, vrfPredicates);
    reachabilityDB.build();
    //    postProcessReachabilityTree();

    timer.end(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();
  }

  void postProcessReachabilityTree() {
    if (!apvMethod) {
      reachabilityDB
          .nodes
          .values()
          .forEach(
              mmap ->
                  mmap.asMap().values().stream()
                      .flatMap(Collection::stream)
                      .forEach(
                          node -> {
                            int packet = node.pkt;
                            for (BddEnvcWrapper.IspVarWrapper var :
                                bddManager.getBddEnvcWrapper().getIspVars().values()) {
                              packet =
                                  bddManager.and(packet, var.getCpDpRelation(bddManager.getBDD()));
                              if (packet == 0) break;
                            }
                            node.pkt = bddManager.and(node.pkt, packet);
                          }));
    }
  }
}
