package datamodel.community;

import atomic.automaton.RegexRepresented;
import com.google.common.base.MoreObjects;
import controlplane.route.DynamicRoute;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.route.HasCommunity;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import javafx.util.Pair;
import main.Controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.Objects;

import static datamodel.community.CommunityRegex.Type.EXACT;
import static datamodel.community.CommunityRegex.Type.REGEX;

/**
 * Representation of a community literal/regex for the symbolic analysis. Configuration languages
 * allow users to match community values using either <b>exact matches</b> or <b>regular
 * expression</b> matches. For example, a regular expression match such as .*:65001 will match any
 * community string that ends with 65001.
 *
 * <p>Currently we support standard, extended, and large community literals, but regexes are assumed
 * by the analysis to only match against standard communities.
 *
 * @author Ryan Beckett
 */
@ParametersAreNonnullByDefault
public final class CommunityRegex extends RegexRepresented
    implements Comparable<CommunityRegex>, CanBeMatched {

  public static final CommunityRegex ALL_STANDARD_COMMUNITIES = CommunityRegex.from(".*");

  private static final Comparator<CommunityRegex> COMPARATOR =
      Comparator.comparing(CommunityRegex::getType)
          .thenComparing(CommunityRegex::getRegex)
          .thenComparing(
              CommunityRegex::getLiteralValue, Comparator.nullsLast(Comparator.naturalOrder()));

  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);

    if (route instanceof HasCommunity) {
      HasCommunity has = (HasCommunity) route;
      Pair<CommunityListIntf, CommunityListIntf> pair = has.getCommunities().match(this);
      if (pair.getKey() != null) {
        R permitted = route.toBuilder().build();
        ((HasCommunity) permitted).setCommunities(pair.getKey());
        result.getPermitted().add(permitted);
      }
      if (pair.getValue() != null) {
        R denied = route.toBuilder().build();
        ((HasCommunity) denied).setCommunities(pair.getValue());
        result.getDenied().add(denied);
      }
      return result;
    }

    result.getDenied().add(route.toBuilder().build());
    return result;
  }

  public enum Type {
    EXACT,
    REGEX
  }

  @Nonnull private final Type _type;
  @Nullable private final Community _literalValue;

  @Nonnull private static final String NUM_REGEX = "(0|[1-9][0-9]*)";

  @Nonnull private static final String STANDARD_COMMUNITY_REGEX = NUM_REGEX + ":" + NUM_REGEX;

  // a regex that represents the syntax of standard community literals supported by Batfish
  // see StandardCommunity::matchString()
  @Nonnull
  private static final String COMMUNITY_REGEX =
      // start-of-string character
      "^"
          + STANDARD_COMMUNITY_REGEX
          // end-of-string character
          + "$";

  /**
   * When converting a community regex to an automaton (see toAutomaton()), we intersect with this
   * automaton, which represents the syntax of standard communities supported by Batfish. Doing so
   * serves two purposes. First, it is necessary for correctness of the symbolic analysis. For
   * example, a regex like ".*" does not actually match any possible string since communities cannot
   * be arbitrary strings. Second, it ensures that when we solve for community literals that match
   * regexes, we will get examples that are sensible and also able to be parsed by Batfish.
   */
  @Nonnull static final Automaton COMMUNITY_FSM = new RegExp(COMMUNITY_REGEX).toAutomaton();

  private CommunityRegex(Type type, String regex, @Nullable Community literalValue) {
    super(regex);
    _type = type;
    _literalValue = literalValue;
  }

  /** Create a community var of type {@link Type#REGEX} */
  public static CommunityRegex from(String regex) {
    return new CommunityRegex(REGEX, regex, null);
  }

  /**
   * Create a community var of type {@link Type#EXACT} based on a literal {@link Community} value
   */
  public static CommunityRegex from(Community literalCommunity) {
    return new CommunityRegex(EXACT, "^" + literalCommunity.matchString() + "$", literalCommunity);
  }

  @Nonnull
  public Type getType() {
    return _type;
  }

  @Nullable
  public Community getLiteralValue() {
    return _literalValue;
  }

  /**
   * Convert this community variable into an equivalent finite-state automaton.
   *
   * @return the automaton
   */
  @Override
  public Automaton toAutomaton() {
    String regex = _regex;
    if (_type == EXACT) {
      return new RegExp(regex).toAutomaton();
    } else {
      /**
       * A regex need only match a portion of a given community string. For example, the regex
       * "^40:" matches the community 40:11. But to properly relate community regexes to one
       * another, for example to find their intersection, we need regexes that match completely.
       *
       * <p>The simple approach below converts a possibly-partial regex into a complete one. It
       * works because we then intersect the resulting automaton with COMMUNITY_FSM, which notably
       * includes the start-of-string and end-of-string characters. Note that the automaton library
       * treats these as ordinary characters.
       *
       * <p>For example, the regex "^40:" becomes ".*(^40:).*", and the final automaton after
       * intersecting with COMMUNITY_FSM accepts the language of the regex "^40:[0-9]+$" as desired.
       */
      regex = ".*" + "(" + regex + ")" + ".*";
      return new RegExp(regex).toAutomaton().intersection(COMMUNITY_FSM);
    }
  }

  private int dd = -1;

  public int getDd() {
    if (dd == -1) {
      dd = Controller.cpExecutor.communityManager.encodeOrigin(this);
    }
    return dd;
  }

  private static final Automaton ARBITRARY_COMMUNITY =
      new RegExp(STANDARD_COMMUNITY_REGEX).toAutomaton();
  private static final Automaton ARBITRARY_COMMUNITY_PREFIX =
      new RegExp("(" + STANDARD_COMMUNITY_REGEX + ", )*").toAutomaton();
  private static final Automaton ARBITRARY_COMMUNITY_SUFFIX =
      new RegExp("(, " + STANDARD_COMMUNITY_REGEX + ")*").toAutomaton();
  private Automaton _listAutomaton;

  public Automaton toListAutomaton() {
    // todo check correctness
    if (_listAutomaton == null) {
      String regex = _regex;

      if (regex.startsWith("^")) {
        regex = regex.substring(1);
      } else {
        regex = ".*" + regex;
      }
      if (regex.endsWith("$")) {
        regex = regex.substring(0, regex.length() - 1);
      } else {
        regex = regex + ".*";
      }

      Automaton a = new RegExp(regex).toAutomaton().intersection(ARBITRARY_COMMUNITY);

      // Encode the meaning that the community list contains this literal community
      // That is, there are (1) any number of standard community and space before the regex, and
      // (2) any number of space and standard community after the regex.
      _listAutomaton =
          (ARBITRARY_COMMUNITY_PREFIX.concatenate(a).concatenate(ARBITRARY_COMMUNITY_SUFFIX));
    }
    return _listAutomaton;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", _type)
        .add("regex", _regex)
        .add("literalValue", _literalValue)
        .toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommunityRegex)) {
      return false;
    }
    CommunityRegex that = (CommunityRegex) o;
    return _type == that._type
        && _regex.equals(that._regex)
        && Objects.equals(_literalValue, that._literalValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type.ordinal(), _regex, _literalValue);
  }

  @Override
  public int compareTo(CommunityRegex that) {
    return COMPARATOR.compare(this, that);
  }
}
