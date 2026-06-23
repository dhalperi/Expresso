package datamodel.path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/* class for OSPF-Route-PropagationPath and BGP-Route-ASPath */
public class Path implements Comparable<Path> {
  private static final String PROP_HOPS = "hops";

  private final ImmutableList<PathHop> path;

  public Path() {
    this.path = ImmutableList.of();
  }

  public Path(Path another) {
    this.path = ImmutableList.copyOf(another.path);
  }

  public Path(PathHop... hops) {
    this(Arrays.asList(hops));
  }

  public Path(@NotNull Collection<PathHop> hops) {
    this.path = ImmutableList.copyOf(hops);
  }

  public Path(Path another, PathHop... hops) {
    ImmutableList.Builder<PathHop> builder = ImmutableList.builder();
    builder.addAll(another.getHops());
    builder.addAll(Arrays.asList(hops));
    this.path = builder.build();
  }

  public Path append(PathHop hop) {
    return new Path(this, hop);
  }

  public PathHop get(int idx) {
    return path.get(idx);
  }

  public boolean contains(PathHop hop) {
    return path.contains(hop);
  }

  public int size() {
    return path.size();
  }

  @JsonIgnore
  public Optional<PathHop> getOrigin() {
    return Optional.ofNullable(Iterables.getFirst(path, null));
  }

  @JsonProperty(PROP_HOPS)
  public List<PathHop> getHops() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Path path1 = (Path) o;
    return Objects.equals(path, path1.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    List<String> list = path.stream().map(PathHop::hopString).collect(Collectors.toList());
    return "[" + String.join(", ", list) + "]";
  }

  @Override
  public int compareTo(@NotNull Path o) {
    return Integer.compare(this.path.size(), o.path.size());
  }
}
