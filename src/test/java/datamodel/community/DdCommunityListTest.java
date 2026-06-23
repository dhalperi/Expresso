package datamodel.community;

import atomic.AtomicPredicates;
import atomic.automaton.AutomatonAtomicPredicates;
import bdd.BddAtomManager;
import com.google.common.collect.ImmutableSet;
import controlplane.network.CPExecutor;
import datamodel.routepolicy.action.SetCommunity;
import dk.brics.automaton.Automaton;
import javafx.util.Pair;
import main.Controller;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class DdCommunityListTest {
  static final String commLiteral1 = "65201:12011";
  static final String commLiteral2 = "65211:12011";
  static final String commRegex = "^652..:12011";
  static final Community comm1 = StandardCommunity.parse(commLiteral1);
  static final Community comm2 = StandardCommunity.parse(commLiteral2);
  static final CommunityRegex commRe1 = CommunityRegex.from(comm1);
  static final CommunityRegex commRe2 = CommunityRegex.from(comm2);
  static final CommunityRegex commRe3 = CommunityRegex.from(commRegex);
  static final AtomicPredicates<CommunityRegex, Automaton> commAps =
      new AutomatonAtomicPredicates<>(
          ImmutableSet.of(commRe1, commRe2, commRe3), CommunityRegex.ALL_STANDARD_COMMUNITIES);
  static final BddAtomManager<CommunityRegex, Automaton> commManager =
      new BddAtomManager<>(commAps);

  static {
    Controller.pushCPExecutor(new CPExecutor());
    Controller.cpExecutor.communityManager = commManager;
  }

  @Test
  public void testSetCommunities() {
    CommunityListIntf list = new DdCommunityList(commManager.empty());
    list = list.setCommunities(SetCommunity.replace(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral1));

    list = list.setCommunities(SetCommunity.replace(comm2));
    assertEquals(list.toString(), String.format("[%s]", commLiteral2));
  }

  @Test
  public void testAddCommunities() {
    CommunityListIntf list = new DdCommunityList(commManager.empty());
    assertEquals(list.toString(), "[]");
    list = list.addCommunities(SetCommunity.increase(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral1));

    list = new DdCommunityList(commManager.encodeOnlyOrigins(Collections.singleton(commRe1)));
    list = list.addCommunities(SetCommunity.increase(comm2));
    assertEquals(list.toString(), String.format("[%s, %s]", commLiteral1, commLiteral2));

    list = new DdCommunityList(commManager.encodeOnlyOrigins(Collections.singleton(commRe2)));
    list = list.addCommunities(SetCommunity.increase(comm1));
    assertEquals(list.toString(), String.format("[%s, %s]", commLiteral1, commLiteral2));
  }

  @Test
  public void testDeleteCommunities() {
    CommunityListIntf list = new DdCommunityList(commManager.empty());
    assertEquals(list.toString(), "[]");
    list = list.deleteCommunities(SetCommunity.decrease(comm1));
    assertEquals(list.toString(), "[]");

    list = new DdCommunityList(commManager.encodeOnlyOrigins(Collections.singleton(commRe1)));
    list = list.deleteCommunities(SetCommunity.decrease(comm1));
    assertEquals(list.toString(), "[]");

    list = new DdCommunityList(commManager.encodeOnlyOrigins(Collections.singleton(commRe2)));
    list = list.deleteCommunities(SetCommunity.decrease(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral2));

    list =
        new DdCommunityList(
            commManager.encodeOnlyOrigins(Stream.of(commRe1, commRe2).collect(Collectors.toSet())));
    list = list.deleteCommunities(SetCommunity.decrease(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral2));
  }

  @Test
  public void testContains() {
    CommunityListIntf list =
        new DdCommunityList(commManager.encodeOnlyOrigins(Collections.singleton(commRe1)));
    System.out.println(list);
    Pair<CommunityListIntf, CommunityListIntf> pair = list.match(commRe1);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    System.out.println("----\n");

    list = list.addCommunities(SetCommunity.increase(comm2));
    System.out.println(list);
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    System.out.println("----\n");

    list = list.setCommunities(SetCommunity.replace(comm2));
    System.out.println(list);
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    System.out.println("----\n");

    list = DdCommunityList.ARBITRARY;
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
  }

  @Test
  public void testComputeAtoms() {
    CommunityRegex regex = CommunityRegex.from("^652..:12011");
    CommunityRegex exact = CommunityRegex.from(StandardCommunity.parse("65201:12011"));
    AutomatonAtomicPredicates<CommunityRegex> aps =
        new AutomatonAtomicPredicates<>(
            ImmutableSet.of(regex, exact), CommunityRegex.ALL_STANDARD_COMMUNITIES);
    aps.getOriginToAtoms()
        .forEach(
            (origin, atoms) ->
                System.out.printf(
                    "origin: %s, atoms: [%s]\n",
                    origin.toString(),
                    atoms.stream().map(String::valueOf).collect(Collectors.joining(", "))));
  }
}
