package datamodel.aspath;

import atomic.automaton.RegexRepresented;
import controlplane.route.DynamicRoute;
import controlplane.route.Route;
import controlplane.route.builder.DynamicRouteBuilder;
import datamodel.CanBeMatched;
import datamodel.route.HasAsPath;
import datamodel.route.RouteFilterEnvironment;
import datamodel.route.RouteFilterResult;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import main.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
public class AsPathRegex extends RegexRepresented implements Comparable<AsPathRegex>, CanBeMatched {

  public static final AsPathRegex ALL_AS_PATHS = new AsPathRegex(".*");

  @Nonnull static final String AS_NUM_REGEX = "(0|[1-9][0-9]*)";

  /**
   * A regex that represents the language of AS paths: a space-separated list of AS numbers,
   * starting and ending with the ^ (start-of-string) and $ (end-of-string) tokens respectively. For
   * non-empty AS-path regexes we require two ^ characters at the front. This is because of the way
   * that Juniper AS-path regexes are translated to Java. For example, "^80$" is translated to "^(^|
   * )80$". Because the automaton library treats ^ as a regular character we have to deal with the
   * fact that there can be two of them at the front. We will therefore arrange that there are
   * _always_ two of them at the front, regardless of what the original Java regex looks like (see
   * toAutomaton below), so we can properly compare regexes to one another.
   *
   * <p>Note: in general an AS path is a list of *sets* of AS numbers. but the format of regexes
   * over sets is apparently vendor-dependent. for now we do not support them.
   */
  @Nonnull
  private static final String AS_PATH_REGEX =
      // the empty AS-path
      "^^$"
          + "|"
          // non-empty AS-paths
          + "^^"
          + "("
          + AS_NUM_REGEX
          + Configuration.ASN_SEPARATOR
          + ")*"
          + AS_NUM_REGEX
          + "$";

  /**
   * When converting an AS path regex to an automaton (see toAutomaton()), we intersect with this
   * automaton, which represents the language of AS paths. Doing so serves several purposes. First,
   * it is necessary for correctness of the symbolic analysis. For example, a regex like ".*" does
   * not actually match any possible string since AS paths cannot be arbitrary strings. Second, it
   * addresses the issue of different formats for Java regexes mentioned above. Third, it ensures
   * that when we solve for AS paths that match regexes, we will get examples that are sensible and
   * also able to be parsed by Batfish.
   */
  @Nonnull static final Automaton AS_PATH_FSM = new RegExp(AS_PATH_REGEX).toAutomaton();

  public AsPathRegex(String regex) {
    super(regex.replace("(,|\\{|\\}|^|$| )", "_").replace(" ", "_"));
  }

  Automaton _automaton;

  /**
   * Convert this community variable into an equivalent finite-state automaton.
   *
   * @return the automaton
   */
  @Override
  public Automaton toAutomaton() {
    if (_automaton == null) {
      /**
       * A regex need only match a portion of a given AS-path string. For example, the regex "_40_"
       * matches AS paths that contain the AS number 40 anywhere. But to properly relate AS paths to
       * one another, for example to find their intersection, we need regexes that match completely.
       *
       * <p>The simple approach below converts a possibly-partial regex into a complete one. It
       * works because below we intersect the resulting automaton with AS_PATH_FSM, which notably
       * includes the start-of-string and end-of-string characters. Note that the automaton library
       * treats these as ordinary characters.
       */
      String regex = ".*" + "(" + _regex + ")" + ".*";
      _automaton = new RegExp(regex).toAutomaton().intersection(AS_PATH_FSM);
    }
    return _automaton;
  }

  Automaton _matchingAutomaton;

  public Automaton toMatchingAutomaton() {
    if (_matchingAutomaton == null) {
      String re = _regex;
      if (!_regex.startsWith("^")) {
        re = ".*" + re;
      } else {
        re = re.substring(1);
      }
      if (!_regex.endsWith("$")) {
        re = re + ".*";
      } else {
        re = re.substring(0, re.length() - 1);
      }
      _matchingAutomaton = new RegExp(re).toAutomaton();
    }
    return _matchingAutomaton;
  }

  @Override
  public String toString() {
    return _regex;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AsPathRegex)) {
      return false;
    }
    AsPathRegex that = (AsPathRegex) o;
    return _regex.equals(that._regex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_regex);
  }

  @Override
  public int compareTo(AsPathRegex that) {
    return _regex.compareTo(that._regex);
  }

  // todo what if the as path regex contains dotted as numbers (e.g., 55990.100)?
  @Override
  public <B extends DynamicRouteBuilder<B, R>, R extends DynamicRoute<B, R>>
      RouteFilterResult<R> filter(R route, RouteFilterEnvironment<R> environment) {
    RouteFilterResult<R> result = new RouteFilterResult<>(route);

    // use concrete as path
    if (route instanceof HasAsPath) {
      AsPathIntf asPath = ((HasAsPath) route).getAsPath();
      if (asPath instanceof AsPath) {
        String asString = asPath.getAsString();
        R cloned = route.toBuilder().build();
        if (Pattern.compile(_regex).matcher(asString).matches()) {
          result.getPermitted().add(cloned);
        } else {
          result.getDenied().add(cloned);
        }
      } else if (asPath instanceof SymbolicAsPath) {
        Automaton asAutomaton = asPath.getAsAutomaton();
        Automaton permitted = toMatchingAutomaton().intersection(asAutomaton);
        Automaton denied = asAutomaton.minus(permitted);
        if (!permitted.isEmpty()) {
          R cloned = Route.clone(route);
          ((HasAsPath) cloned).setAsPath(new SymbolicAsPath(permitted));
          result.getPermitted().add(cloned);
        }
        if (!denied.isEmpty()) {
          R cloned = Route.clone(route);
          ((HasAsPath) cloned).setAsPath(new SymbolicAsPath(denied));
          result.getDenied().add(cloned);
        }
      }
      return result;
    }

    result.getPermitted().add(route.toBuilder().build());
    return result;
  }

  private static final Map<Integer, AsPathRegex> LENGTH_MAP = new TreeMap<>();

  public static AsPathRegex ofLength(int length) {
    if (length < 0) {
      throw new IllegalArgumentException();
    }
    return LENGTH_MAP.computeIfAbsent(
        length,
        l -> {
          String re =
              l == 0
                  ? ""
                  : String.format(
                      "%s(%s%s){0,%d}",
                      AS_NUM_REGEX, Configuration.ASN_SEPARATOR, AS_NUM_REGEX, length - 1);
          return new AsPathRegex(re);
        });
  }
}
