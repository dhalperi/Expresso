package datamodel.aspath;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import main.ExpressoLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static datamodel.aspath.AsPathRegex.AS_NUM_REGEX;
import static main.Configuration.ASN_SEPARATOR;

public final class SymbolicAsPath implements AsPathIntf, Comparable<SymbolicAsPath> {
  final Automaton symbolicAsPath;
  final int length;

  public SymbolicAsPath(String regex) {
    this(regexToAutomaton(regex));
  }

  public SymbolicAsPath(Automaton automaton) {
    symbolicAsPath = automaton;
    length = findMinLength(symbolicAsPath);
  }

  public SymbolicAsPath(SymbolicAsPath another) {
    symbolicAsPath = another.symbolicAsPath.clone();
    length = another.length;
  }

  private static final Automaton ASN = new RegExp(AS_NUM_REGEX).toAutomaton();
  private static final Automaton SEPARATOR = Automaton.makeString(ASN_SEPARATOR);
  private static final String AS_PATH_REGEX =
      String.format("%s(%s%s)*", AS_NUM_REGEX, ASN_SEPARATOR, AS_NUM_REGEX);
  private static final Automaton AS_PATH =
      Automaton.makeEmptyString().union(new RegExp(AS_PATH_REGEX).toAutomaton());
  private static final Map<Integer, Automaton> AS_PATH_MAP = new TreeMap<>();

  private static Automaton ofLength(int length) {
    return AS_PATH_MAP.computeIfAbsent(
        length,
        x ->
            x == 0
                ? Automaton.makeEmptyString()
                : (x == 1 ? ASN : ofLength(length - 1).concatenate(SEPARATOR).concatenate(ASN)));
  }

  public static int findMinLength(Automaton automaton) {
    int l = 0;
    while (true) {
      Automaton length = ofLength(l);
      if (length.intersection(automaton).isEmpty()) l++;
      else break;
    }
    return l;
  }

  public static SymbolicAsPath ofSingletonAsSets(Long... asNums) {
    return ofSingletonAsSets(Arrays.asList(asNums));
  }

  public static SymbolicAsPath ofSingletonAsSets(List<Long> asNums) {
    String regex =
        asNums.stream()
            .map(asn -> AsSet.of(asn).toString())
            .collect(Collectors.joining(ASN_SEPARATOR));
    return new SymbolicAsPath(regex);
  }

  private static final SymbolicAsPath EMPTY = new SymbolicAsPath("");
  private static final SymbolicAsPath ANY = new SymbolicAsPath(".*");

  public static SymbolicAsPath empty() {
    return EMPTY;
  }

  public static SymbolicAsPath any() {
    return ANY;
  }

  @Override
  public SymbolicAsPath removePrivateAs() {
    ExpressoLogger.log(
        ExpressoLogger.LEVEL.DEBUG,
        "Haven't support removing private AS numbers from a symbolic as path");
    return new SymbolicAsPath(this);
  }

  @Nonnull
  @Override
  public SymbolicAsPath removeAsns(Collection<Long> asns) {
    ExpressoLogger.log(
        ExpressoLogger.LEVEL.DEBUG, "Haven't support removing AS numbers from a symbolic as path");
    return new SymbolicAsPath(this);
  }

  @Override
  public SymbolicAsPath append(List<AsSet> asSets) {
    Automaton append =
        regexToAutomaton(
            asSets.stream().map(AsSet::toString).collect(Collectors.joining(ASN_SEPARATOR)));
    if (!symbolicAsPath.subsetOf(EMPTY.symbolicAsPath)) {
      append = append.concatenate(SEPARATOR).concatenate(symbolicAsPath);
    }
    return new SymbolicAsPath(append);
  }

  @Override
  public SymbolicAsPath overwrite(List<AsSet> asSets) {
    Automaton overwrite =
        regexToAutomaton(
            asSets.stream().map(AsSet::toString).collect(Collectors.joining(ASN_SEPARATOR)));
    return new SymbolicAsPath(overwrite);
  }

  /**
   * If {@code n} equals 0, we compute the difference ({@link Automaton#minus(Automaton)}) of {@link
   * SymbolicAsPath#symbolicAsPath} to the regex encoded by {@link SymbolicAsPath#containsRe(long)}.
   * <br>
   * Otherwise, we compute the intersection ({@link Automaton#intersection(Automaton)}) of {@link
   * SymbolicAsPath#symbolicAsPath} and the regex encoded by {@link SymbolicAsPath#leRe(long, int)}.
   *
   * @param asn an ASN.
   * @param n the repetition limitation of {@param asn} in this {@link SymbolicAsPath}.
   * @return a new {@link SymbolicAsPath} that satisfies the repetition limitation, or null if none
   *     of the concrete AS paths contained by this {@link SymbolicAsPath} satisfies the limitation.
   */
  @Override
  @Nullable
  public SymbolicAsPath allowAsLoop(long asn, int n) {
    // todo check correctness
    Automaton automaton, allowed;
    if (n == 0) {
      automaton = containsAutomaton(asn);
      allowed = symbolicAsPath.minus(automaton);
    } else {
      automaton = leAutomaton(asn, n);
      allowed = symbolicAsPath.intersection(automaton);
    }
    return allowed.isEmpty() ? null : new SymbolicAsPath(allowed);
  }

  /**
   * The regex is encoded by {@link SymbolicAsPath#containsRe(long)}.
   *
   * @param as an ASN
   * @return whether this {@link SymbolicAsPath} contains the specified ASN {@param as}.
   */
  @Override
  public boolean containsAs(Long as) {
    Automaton contains = containsAutomaton(as);
    return symbolicAsPath.subsetOf(contains);
  }

  /**
   * The regex is encoded as a union of "" and "asns(_asns)*", where "" means empty AS path, and
   * "asns(_asns)*" means ASNs listed in {@code asns} appear one or multiple times.
   *
   * @param asns ASNs
   * @return whether this {@link SymbolicAsPath} contains only ASNs specified by {@param asns}
   */
  @Override
  public boolean containsOnly(Collection<Long> asns) {
    String group =
        String.format(
            "(%s)",
            asns.stream().map(AsSet::of).map(AsSet::toString).collect(Collectors.joining("|")));
    String none = "";
    String multiple = String.format("%s(%s%s)*", group, ASN_SEPARATOR, group);
    Automaton containsOnly = regexToAutomaton(none).union(regexToAutomaton(multiple));
    return symbolicAsPath.subsetOf(containsOnly);
  }

  @Override
  public String getAsString() {
    return symbolicAsPath.getShortestExample(true);
  }

  @Override
  public Automaton getAsAutomaton() {
    return symbolicAsPath;
  }

  @Override
  public int length() {
    return length;
  }

  @VisibleForTesting
  static String containsRe(long asn) {
    String singleton = AsSet.of(asn).toString();
    return containsRe(singleton);
  }

  /**
   * @param singleton can be either an ASN (e.g. 100) or a group of possible ASNs (e.g.,
   *     100|200|300)
   * @return a regex representing >= 1 repeats of {@param singleton}. It is encoded as the
   *     conjunction of a regex that contains only {@param singleton}, a regex that starts with
   *     {@param singleton}, a regex that ends with {@param singleton}, and a regex that has {@param
   *     singleton} in the middle.
   */
  @VisibleForTesting
  static String containsRe(String singleton) {
    String startsWith = singleton + ASN_SEPARATOR + ".+";
    String endsWith = ".+" + ASN_SEPARATOR + singleton;
    String inMiddle = ".+" + ASN_SEPARATOR + singleton + ASN_SEPARATOR + ".+";
    return String.format("%s|%s|%s|%s", singleton, startsWith, endsWith, inMiddle);
  }

  @VisibleForTesting
  static String leRe(long asn, int n) {
    // todo check correctness
    String asSet = AsSet.of(asn).toString();
    String others = notRe(asn);
    String exclude = excludeRe(asn);
    return String.format(
        "%s(%s((%s%s)*%s%s){0,%d})?%s",
        exclude, asSet, ASN_SEPARATOR, others, ASN_SEPARATOR, asSet, n - 1, exclude);
  }

  /** Encode an ASN regex representing all ASNs except {@code asn}. */
  @VisibleForTesting
  static String notRe(long asn) {
    // todo check correctness
    String asSet = AsSet.of(asn).toString();
    return String.format("(~(%s))", asSet);
  }

  /** Encode an AS path regex representing AS paths that doesn't contain {@code asn}. */
  @VisibleForTesting
  static String excludeRe(long asn) {
    // todo check correctness
    String not = notRe(asn);
    return String.format("(%s(%s%s)*)?", not, ASN_SEPARATOR, not);
  }

  @VisibleForTesting
  static Automaton containsAutomaton(long asn) {
    return regexToAutomaton(containsRe(asn));
  }

  @VisibleForTesting
  static Automaton containsAutomaton(String singleton) {
    return regexToAutomaton(containsRe(singleton));
  }

  @VisibleForTesting
  static Automaton leAutomaton(long asn, int n) {
    Automaton as = Automaton.makeString(AsSet.of(asn).toString());
    Automaton exclude = excludeAutomaton(asn);
    Automaton not = notAutomaton(asn);
    Automaton noMoreThan =
        (as.concatenate(
                (((SEPARATOR.concatenate(not)).repeat()).concatenate(SEPARATOR.concatenate(as)))
                    .repeat(0, n - 1)))
            .optional();
    return exclude.concatenate(noMoreThan).concatenate(exclude);
  }

  @VisibleForTesting
  static Automaton notAutomaton(long asn) {
    Automaton a1 = Automaton.makeString(AsSet.of(asn).toString());
    return ASN.minus(a1);
  }

  @VisibleForTesting
  static Automaton excludeAutomaton(long asn) {
    Automaton contains = containsAutomaton(asn);
    return AS_PATH.minus(contains);
  }

  @VisibleForTesting
  static Automaton regexToAutomaton(String regex) {
    return new RegExp(regex).toAutomaton().intersection(AS_PATH);
  }

  @Override
  public int compareTo(@Nonnull SymbolicAsPath o) {
    return Comparator.comparing(SymbolicAsPath::length)
        .thenComparing(s -> s.symbolicAsPath.getShortestExample(true))
        .compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SymbolicAsPath that = (SymbolicAsPath) o;
    return length == that.length && Objects.equals(symbolicAsPath, that.symbolicAsPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbolicAsPath, length);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("str", symbolicAsPath.getShortestExample(true))
        .add("len", length)
        .toString();
  }
}
