package datamodel.community;

import datamodel.routepolicy.action.SetCommunity;
import javafx.util.Pair;
import main.Controller;

import java.util.Objects;

public class DdCommunityList implements CommunityListIntf {
  public static final DdCommunityList ARBITRARY = new DdCommunityList(1);

  private final int dd;

  public DdCommunityList(int d) {
    dd = d;
  }

  public int getDd() {
    return dd;
  }

  @Override
  public CommunityListIntf setCommunities(SetCommunity setCommunity) {
    int i = Controller.cpExecutor.communityManager.set(setCommunity.collectCommunityRegexes());
    return new DdCommunityList(i);
  }

  @Override
  public CommunityListIntf addCommunities(SetCommunity setCommunity) {
    int i = Controller.cpExecutor.communityManager.add(dd, setCommunity.collectCommunityRegexes());
    return new DdCommunityList(i);
  }

  @Override
  public CommunityListIntf deleteCommunities(SetCommunity setCommunity) {
    // todo check correctness
    int i =
        Controller.cpExecutor.communityManager.delete(dd, setCommunity.collectCommunityRegexes());
    return new DdCommunityList(i);
  }

  @Override
  public String toString() {
    return Controller.cpExecutor.communityManager.getString(dd);
  }

  @Override
  public Pair<CommunityListIntf, CommunityListIntf> match(CommunityRegex communityRegex) {
    int permitted = Controller.cpExecutor.communityManager.contains(dd, communityRegex);
    int denied = Controller.cpExecutor.communityManager.diff(dd, permitted);

    return new Pair<>(
        permitted == 0 ? null : new DdCommunityList(permitted),
        denied == 0 ? null : new DdCommunityList(denied));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DdCommunityList that = (DdCommunityList) o;
    return dd == that.dd;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dd);
  }
}
