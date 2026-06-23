package datamodel.community;

import com.google.common.collect.ImmutableList;
import datamodel.routepolicy.action.SetCommunity;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class CommunityList implements CommunityListIntf {
  public static final CommunityList EMPTY = new CommunityList(ImmutableList.of());

  private final List<Community> communities;

  public CommunityList(List<Community> communities) {
    this.communities =
        communities instanceof ImmutableList ? communities : ImmutableList.copyOf(communities);
  }

  public CommunityList(CommunityList another) {
    communities = ImmutableList.copyOf(another.communities);
  }

  @Override
  public CommunityListIntf setCommunities(SetCommunity setCommunity) {
    return new CommunityList(setCommunity.getCommunities());
  }

  @Override
  public CommunityListIntf addCommunities(SetCommunity setCommunity) {
    ImmutableList.Builder<Community> newCommunities = ImmutableList.builder();
    newCommunities.addAll(communities);
    newCommunities.addAll(setCommunity.getCommunities());
    return new CommunityList(newCommunities.build());
  }

  @Override
  public CommunityListIntf deleteCommunities(SetCommunity setCommunity) {
    ImmutableList.Builder<Community> newCommunities = ImmutableList.builder();
    communities.stream()
        .filter(comm -> !setCommunity.getCommunities().contains(comm))
        .forEach(newCommunities::add);
    return new CommunityList(newCommunities.build());
  }

  @Override
  public String toString() {
    return String.format(
        "[%s]",
        communities.stream().sorted().map(Community::toString).collect(Collectors.joining(", ")));
  }

  @Override
  public Pair<CommunityListIntf, CommunityListIntf> match(CommunityRegex communityRegex) {
    Pattern pattern = Pattern.compile(communityRegex.getRegex());
    boolean matched =
        firstNonNull(communities, new ArrayList<>()).stream()
            .anyMatch(community -> pattern.matcher(community.toString()).matches());
    CommunityList copy = new CommunityList(this);
    return new Pair<>(matched ? copy : null, matched ? null : copy);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommunityList that = (CommunityList) o;
    return Objects.equals(communities, that.communities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(communities);
  }
}
