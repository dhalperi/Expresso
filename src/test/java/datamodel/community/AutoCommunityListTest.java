package datamodel.community;

import datamodel.routepolicy.action.SetCommunity;
import javafx.util.Pair;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static datamodel.community.DdCommunityListTest.comm1;
import static datamodel.community.DdCommunityListTest.comm2;
import static datamodel.community.DdCommunityListTest.commLiteral1;
import static datamodel.community.DdCommunityListTest.commLiteral2;
import static datamodel.community.DdCommunityListTest.commRe1;
import static datamodel.community.DdCommunityListTest.commRe2;
import static datamodel.community.DdCommunityListTest.commRe3;
import static org.junit.Assert.assertEquals;

public class AutoCommunityListTest {
  @Test
  public void testSetCommunities() {
    CommunityListIntf list = AutoCommunityList.EMPTY;
    list = list.setCommunities(SetCommunity.replace(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral1));

    list = list.setCommunities(SetCommunity.replace(comm2));
    assertEquals(list.toString(), String.format("[%s]", commLiteral2));
  }

  @Test
  public void testAddCommunities() {
    CommunityListIntf list = AutoCommunityList.EMPTY;
    assertEquals(list.toString(), "[]");

    list = list.addCommunities(SetCommunity.increase(comm1));
    assertEquals(list.toString(), String.format("[%s]", commLiteral1));

    list = new AutoCommunityList(commRe1.toListAutomaton());
    list = list.addCommunities(SetCommunity.increase(comm2));
    assertEquals(list.toString(), String.format("[%s, %s]", commLiteral1, commLiteral2));

    list = new AutoCommunityList(commRe2.toListAutomaton());
    list = list.addCommunities(SetCommunity.increase(comm1));
    assertEquals(list.toString(), String.format("[%s, %s]", commLiteral1, commLiteral2));
  }

  @Test
  public void testContains() {
    CommunityListIntf list = new AutoCommunityList(commRe1.toListAutomaton());
    System.out.println(list);
    Pair<CommunityListIntf, CommunityListIntf> pair = list.match(commRe1);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    separate();

    list = list.addCommunities(SetCommunity.increase(comm2));
    System.out.println(list);
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    separate();

    list = list.setCommunities(SetCommunity.replace(comm2));
    System.out.println(list);
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    separate();

    list = AutoCommunityList.ARBITRARY;
    pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
  }

  @Test
  public void testMatch() {
    CommunityListIntf list = AutoCommunityList.ARBITRARY;

    Pair<CommunityListIntf, CommunityListIntf> pair = list.match(commRe3);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    separate();

    pair = pair.getKey().match(commRe1);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
    separate();

    pair = pair.getValue().match(commRe1);
    if (pair.getKey() != null) System.out.println("permit: " + pair.getKey().toString());
    if (pair.getValue() != null) System.out.println("denied: " + pair.getValue().toString());
  }

  private static void separate() {
    System.out.println("--------\n");
  }
}
