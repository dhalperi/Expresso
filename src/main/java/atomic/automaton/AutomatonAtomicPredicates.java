package atomic.automaton;

import atomic.AtomicPredicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import datamodel.community.CommunityRegex;
import dk.brics.automaton.Automaton;
import main.ExpressoLogger;
import org.apache.commons.lang3.ObjectUtils;
import org.batfish.common.BatfishException;
import util.TimeUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class to compute atomic predicates for a set of regular expressions. This is used by the
 * symbolic analysis to reason about community regexes and also AS-path regexes.
 *
 * <p>The atomic predicates are the minimal set of predicates such that: 1. no atomic predicate is
 * logically false; 2. the disjunction of all atomic predicates is logically true; 3. each atomic
 * predicate is disjoint from all others; 4. each regex is equivalent to a disjunction of some
 * subset of the atomic predicates.
 *
 * <p>The idea of atomic predicates comes from the paper "Real-time Verification of Network
 * Properties using Atomic Predicates" by Yang and Lam, IEEE/ACM Transactions on Networking, April
 * 2016, Volume 24, No. 2, pages 887-900.
 * http://www.cs.utexas.edu/users/lam/Vita/Jpapers/Yang_Lam_TON_2015.pdf In that paper, they create
 * atomic predicates in order to precisely and scalably analyze packet forwarding symbolically; we
 * use the same idea to track regexes in symbolic routing analysis.
 *
 * @param <T> the particular type of regexes
 */
@ParametersAreNonnullByDefault
public class AutomatonAtomicPredicates<T extends AutomatonRepresented>
    extends AtomicPredicates<T, Automaton> {
  /**
   * Create atomic predicates for the given set of regexes.
   *
   * @param regexes the regexes
   * @param trueRegex a regex representing logical "true", or all possible valid strings
   */
  public AutomatonAtomicPredicates(Set<T> regexes, T trueRegex) {
    super(ImmutableSet.<T>builder().addAll(regexes).add(trueRegex).build());
  }

  private static final Comparator<Automaton> CMP_NUM_STATES =
      Comparator.comparingInt(Automaton::getNumberOfStates);
  private static final Comparator<Automaton> CMP_NUM_TRANS =
      Comparator.comparingInt(Automaton::getNumberOfTransitions);
  private static final Comparator<Automaton> CMP_SHORTEST_EXAMPLE =
      Comparator.comparingInt(
          a -> ObjectUtils.firstNonNull(a.getShortestExample(true), "").length());

  @Override
  protected void initAtoms() {
    T one = _origins.iterator().next();
    if (one instanceof CommunityRegex) ExpressoLogger.pushContext("COMMUNITY AP");
    else ExpressoLogger.pushContext("AS PATH AP");

    // there are origins with the same automaton, enumerate over automatons instead of origins to
    // avoid duplicated computation
    Multimap<Automaton, T> autoToOrigin = HashMultimap.create();
    for (T origin : _origins) {
      autoToOrigin.put(origin.toAutomaton(), origin);
    }
    int nAuto = autoToOrigin.asMap().keySet().size();
    int nOrigin = _origins.size();
    if (nAuto < nOrigin) {
      ExpressoLogger.log(
          ExpressoLogger.LEVEL.INFO,
          String.format("has duplication, num origins: %d, num automatons: %d", nOrigin, nAuto));
    }
//    System.out.println(nOrigin);

    TimeUtil timer = new TimeUtil();
    int itr = 1;

    // try to accelerate the computation by sorting the automatons
    Comparator<Automaton> comparator = CMP_SHORTEST_EXAMPLE;
    ExpressoLogger.log(
        ExpressoLogger.LEVEL.INFO,
        "sorting automatons by "
            + (comparator == CMP_NUM_STATES
                ? "number of states."
                : comparator == CMP_NUM_TRANS
                    ? "number of transitions."
                    : "shortest example length."));
    List<Automaton> order =
        autoToOrigin.asMap().keySet().stream().sorted(comparator).collect(Collectors.toList());

    SetMultimap<Automaton, T> mmap = HashMultimap.create();
    for (Automaton auto : order) {
      String msg =
          String.format(
              "round: %s, regex: %s", itr++, autoToOrigin.get(auto).iterator().next().toString());
      timer.begin(ExpressoLogger.LEVEL.DEBUG, msg);
//      System.out.print(msg);

      if (auto.isEmpty()) {
        // empty automaton; give up
        throw new BatfishException("Empty automaton");
      }
      Collection<T> autoOrigins = autoToOrigin.get(auto);

      SetMultimap<Automaton, T> newMMap = HashMultimap.create(mmap);
      for (Automaton atom : mmap.keySet()) {
        if (atom.equals(auto)) {
          newMMap.putAll(atom, autoOrigins);
          // since all atomic predicates are disjoint from one another, if
          // auto is equal to an existing one, we can ignore all the rest
          break;
        }
        Automaton inter = atom.intersection(auto);
        if (inter.isEmpty()) {
          // auto is disjoint from atom, so move on to the next atomic predicate
          continue;
        }
        // replace automaton atom with two new atomic predicates, representing the intersection
        // and difference with auto
        Set<T> atomOrigins = newMMap.removeAll(atom);
        Automaton diff = atom.minus(auto);
        newMMap.putAll(inter, atomOrigins);
        if (!diff.isEmpty()) {
          newMMap.putAll(diff, atomOrigins);
        }
        // add origins to the intersection
        newMMap.putAll(inter, autoOrigins);
        // update auto with the residual automaton that is left
        auto = auto.minus(atom);
        if (auto.isEmpty()) break;
      }
      if (!auto.isEmpty()) {
        // if there's anything left of cAuto by the end, add it
        newMMap.putAll(auto, autoOrigins);
      }
      mmap = newMMap;

      timer.end(ExpressoLogger.LEVEL.DEBUG, "number atoms: " + mmap.keySet().size());
//      System.out.printf(", time: %f, number atoms: %d\n", timer.getTotal(), mmap.keySet().size());
    }

    ExpressoLogger.log(ExpressoLogger.LEVEL.INFO, "finish computing, generating maps");

    initMaps(mmap);

    timer.logStatistic(ExpressoLogger.LEVEL.INFO, "");
    ExpressoLogger.popContext();
  }
}
