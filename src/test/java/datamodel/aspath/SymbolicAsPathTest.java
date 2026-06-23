package datamodel.aspath;

import com.google.common.collect.ImmutableList;
import dk.brics.automaton.Automaton;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class SymbolicAsPathTest {
  @Test
  public void testAppend() {
    SymbolicAsPath asPath = new SymbolicAsPath("");
    asPath = asPath.append(Collections.singletonList(AsSet.of(55990L)));
    assertEquals(asPath.getAsString(), "55990");
    asPath = asPath.append(Collections.singletonList(AsSet.of(65535L)));
    assertEquals(asPath.getAsString(), "65535_55990");
  }

  @Test
  public void testOverwrite() {
    SymbolicAsPath asPath = new SymbolicAsPath("");
    asPath = asPath.overwrite(Collections.singletonList(AsSet.of(55990L)));
    assertEquals(asPath.getAsString(), "55990");
    asPath = asPath.overwrite(Collections.singletonList(AsSet.of(65535L)));
    assertEquals(asPath.getAsString(), "65535");
  }

  @Test
  public void testAllowAsLoop() {
    SymbolicAsPath asPath = new SymbolicAsPath("(55990(_55990){0,4})?");

    SymbolicAsPath allowAsLoop2 = asPath.allowAsLoop(55990, 2);
    assertNotNull(allowAsLoop2);
    System.out.println(allowAsLoop2.symbolicAsPath.toDot());

    SymbolicAsPath allowAsLoop0 = asPath.allowAsLoop(55990, 0);
    assertNotNull(allowAsLoop0);
    assertTrue(allowAsLoop0.symbolicAsPath.subsetOf(Automaton.makeEmptyString()));
    System.out.println(allowAsLoop0.symbolicAsPath.toDot());

    asPath = new SymbolicAsPath("55990(_55990){0,4}");
    allowAsLoop0 = asPath.allowAsLoop(55990, 0);
    assertNull(allowAsLoop0);
  }

  @Test
  public void testContainsAs() {
    SymbolicAsPath asPath = new SymbolicAsPath("65535_55990");

    assertTrue(asPath.containsAs(65535L));
    assertFalse(asPath.containsAs(55991L));
  }

  @Test
  public void testContainsOnly() {
    SymbolicAsPath asPath = new SymbolicAsPath("65535_55990");

    assertTrue(asPath.containsOnly(ImmutableList.of(65535L, 55990L)));
    assertFalse(asPath.containsOnly(ImmutableList.of(65535L)));
  }

  @Test
  public void testNot() {
    String not = SymbolicAsPath.notRe(55990L);
    System.out.println(not);

    Automaton automaton = SymbolicAsPath.notAutomaton(55990L);
    System.out.println(automaton.toDot());

    boolean b = automaton.run("55990");
    assertFalse(b);

    b = automaton.run("55991");
    assertTrue(b);
  }

  @Test
  public void testExclude() {
    String exclude = SymbolicAsPath.excludeRe(1L);
    System.out.println(exclude);

    Automaton automaton = SymbolicAsPath.excludeAutomaton(1L);
    System.out.println(automaton.toDot());

    boolean b = automaton.run("1");
    assertFalse(b);

    b = automaton.run("10");
    assertTrue(b);

    b = automaton.run("2_1");
    assertFalse(b);

    b = automaton.run("2");
    assertTrue(b);

    b = automaton.run("2_3");
    assertTrue(b);
  }
}
