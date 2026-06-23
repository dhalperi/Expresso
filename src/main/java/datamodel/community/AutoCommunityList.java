package datamodel.community;

import datamodel.routepolicy.action.SetCommunity;
import dk.brics.automaton.Automaton;
import javafx.util.Pair;
import main.ExpressoLogger;
import util.AutomatonUtil;

import java.util.Objects;

public class AutoCommunityList implements CommunityListIntf {
  public static final AutoCommunityList EMPTY = new AutoCommunityList(Automaton.makeString(""));
  public static final AutoCommunityList ARBITRARY =
      new AutoCommunityList(Automaton.makeAnyString());

  private final Automaton automaton;

  public AutoCommunityList(Automaton a) {
    automaton = a;
  }

  @Override
  public CommunityListIntf setCommunities(SetCommunity setCommunity) {
    Automaton a = setCommunity.getAutomaton();
    return new AutoCommunityList(a);
  }

  @Override
  public CommunityListIntf addCommunities(SetCommunity setCommunity) {
    Automaton a = setCommunity.getAutomaton();
    if (!EMPTY.equals(this)) a = a.intersection(automaton);
    return new AutoCommunityList(a);
  }

  @Override
  public CommunityListIntf deleteCommunities(SetCommunity setCommunity) {
    ExpressoLogger.log(ExpressoLogger.LEVEL.DEBUG, "Unsupported feature: deleteCommunities.");
    return new AutoCommunityList(automaton);
  }

  @Override
  public Pair<CommunityListIntf, CommunityListIntf> match(CommunityRegex communityRegex) {
    Automaton a1 = communityRegex.toListAutomaton();
    Automaton a2 = automaton;

    Automaton permitted = a1.intersection(a2);
    Automaton denied = a2.minus(permitted);

    return new Pair<>(
        permitted.isEmpty() ? null : new AutoCommunityList(permitted),
        denied.isEmpty() ? null : new AutoCommunityList(denied));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AutoCommunityList that = (AutoCommunityList) o;
    return Objects.equals(automaton, that.automaton);
  }

  @Override
  public int hashCode() {
    return Objects.hash(automaton);
  }

  @Override
  public String toString() {
    String example = automaton.getShortestExample(true);
    if (example == null) return null;
    return String.format("[%s]", AutomatonUtil.getShortestExample(automaton, true));
  }
}
