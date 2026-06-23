package datamodel.routepolicy.action;

import com.google.common.collect.ImmutableList;
import controlplane.route.Route;
import datamodel.aspath.AsPathFactory;
import datamodel.aspath.AsPathIntf;
import datamodel.aspath.AsPathRegex;
import datamodel.aspath.AsSet;
import datamodel.community.CommunityRegex;
import datamodel.ipv4.PrefixRange;
import datamodel.route.HasAsPath;
import datamodel.route.RouteFilterEnvironment;
import main.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <a
 * href="https://support.huawei.com/enterprise/en/doc/EDOC1000128405/82f1fdfa/apply-as-path">Huawei-doc</a>
 * Most devices only support additive, overwrite and none.
 */
public class SetAsPath implements Action {
  final boolean additive;
  final boolean overwrite;
  final boolean none;
  final boolean delete;
  final List<Long> asns;

  public SetAsPath(
      boolean additive, boolean overwrite, boolean none, boolean delete, List<Long> asns) {
    this.additive = additive;
    this.overwrite = overwrite;
    this.none = none;
    this.delete = delete;
    this.asns = ImmutableList.copyOf(asns);
  }

  public SetAsPath(
      boolean additive, boolean overwrite, boolean none, boolean delete, Long... asns) {
    this.additive = additive;
    this.overwrite = overwrite;
    this.none = none;
    this.delete = delete;
    this.asns = ImmutableList.copyOf(asns);
  }

  public static SetAsPath additive(List<Long> asns) {
    return new SetAsPath(true, false, false, false, asns);
  }

  public static SetAsPath overwrite(List<Long> asns) {
    return new SetAsPath(false, true, false, false, asns);
  }

  public AsPathRegex getAsPathRegex() {
    if (none) {
      return new AsPathRegex("^$");
    } else {
      if (Configuration.ABSTRACT_SET_AS_PATH) {
        List<AsSet> asSets = asns.stream().distinct().map(AsSet::of).collect(Collectors.toList());
        String str = asSets.stream().map(AsSet::toString).collect(Collectors.joining("_"));
        if (overwrite) str = str + "$"; // ends with
        return new AsPathRegex(str);
      } else {
        List<AsSet> asSets = asns.stream().map(AsSet::of).collect(Collectors.toList());
        String str = asSets.stream().map(AsSet::toString).collect(Collectors.joining("_"));
        // todo is this encoding correct?
        if (overwrite) str = str + "$"; // ends with
        return new AsPathRegex(str);
      }
    }
  }

  @Override
  public <R extends Route> void act(R route, RouteFilterEnvironment<R> environment) {
    if (route instanceof HasAsPath) {
      HasAsPath has = (HasAsPath) route;
      if (none) {
        has.setAsPath(AsPathFactory.empty());
      } else if (delete) {
        has.setAsPath(has.getAsPath().removeAsns(asns));
      } else {
        List<AsSet> asSets = asns.stream().map(AsSet::of).collect(Collectors.toList());
        AsPathIntf modified =
            additive ? has.getAsPath().append(asSets) : has.getAsPath().overwrite(asSets);
        has.setAsPath(modified);
      }
    } else {
      throw new IllegalArgumentException("This type of route does not have AS path");
    }
  }

  @Override
  public Set<PrefixRange> collectPrefixRanges() {
    return Collections.emptySet();
  }

  @Override
  public Set<CommunityRegex> collectCommunityRegexes() {
    return Collections.emptySet();
  }

  @Override
  public Set<AsPathRegex> collectAsPathRegexes() {
    return Collections.singleton(getAsPathRegex());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetAsPath setAsPath = (SetAsPath) o;
    return additive == setAsPath.additive
        && overwrite == setAsPath.overwrite
        && none == setAsPath.none
        && delete == setAsPath.delete
        && Objects.equals(asns, setAsPath.asns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additive, overwrite, none, delete, asns);
  }
}
