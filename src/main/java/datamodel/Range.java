package datamodel;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** A range is closed. */
public class Range<T extends Comparable<T>> implements Comparable<Range<T>> {
  private final T lowerBound;
  private final T upperBound;

  public static <T extends Comparable<T>> Range<T> of(T lowerBound, T upperBound) {
    if (lowerBound.compareTo(upperBound) > 0) {
      return null;
    }
    return new Range<>(lowerBound, upperBound);
  }

  public Range(T lowerBound, T upperBound) {
    if (lowerBound.compareTo(upperBound) > 0) {
      throw new IllegalArgumentException();
    }
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  public Range(T bound) {
    this.lowerBound = bound;
    this.upperBound = bound;
  }

  public T getLowerBound() {
    return lowerBound;
  }

  public T getUpperBound() {
    return upperBound;
  }

  public boolean contains(Range<T> that) {
    return lowerBound.compareTo(that.lowerBound) <= 0 && upperBound.compareTo(that.upperBound) >= 0;
  }

  @Override
  public int compareTo(@NotNull Range<T> o) {
    int cl = lowerBound.compareTo(o.lowerBound);
    int cu = upperBound.compareTo(o.upperBound);
    return cl == 0 ? cu : cl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Range<?> range = (Range<?>) o;
    return Objects.equals(lowerBound, range.lowerBound)
        && Objects.equals(upperBound, range.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowerBound, upperBound);
  }

  @Override
  public String toString() {
    return lowerBound + "~" + upperBound;
  }
}
