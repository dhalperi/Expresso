package atomic.bdd;

import atomic.AtomicPredicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import main.Controller;
import main.ExpressoLogger;
import util.TimeUtil;

import java.util.Set;

public class BddAtomicPredicates<T extends BddRepresented> extends AtomicPredicates<T, Integer> {
  /**
   * Create atomic predicates for the given set of origins.
   *
   * @param origins the origins
   */
  public BddAtomicPredicates(Set<T> origins) {
    super(ImmutableSet.<T>builder().addAll(origins).build());
  }

  @Override
  protected void initAtoms() {
    ExpressoLogger.pushContext("PREFIX AP");
    TimeUtil timer = new TimeUtil();
    int itr = 1;

    SetMultimap<Integer, T> aToP = HashMultimap.create();
    for (T origin : _origins) {
      timer.begin();

      int bddRep = origin.toBdd();
      SetMultimap<Integer, T> newMMap = HashMultimap.create(aToP);
      for (Integer a : aToP.keySet()) {
        if (a.equals(bddRep)) {
          newMMap.put(a, origin);
          // since all atomic predicates are disjoint from one another, if
          // this origin is equal to an existing one, we can ignore all the rest
          break;
        }
        int inter = Controller.bddManager.and(a, bddRep);
        if (inter == 0) {
          // this origin is disjoint from a, so move on to the next atomic predicate
          continue;
        }
        // replace automaton a with two new atomic predicates, representing the intersection
        // and difference with origin's automaton
        Set<T> affected = newMMap.removeAll(a);
        int diff = Controller.bddManager.minus(a, bddRep);
        newMMap.putAll(inter, affected);
        if (diff != 0) {
          newMMap.putAll(diff, affected);
        }
        // add origin to the intersection
        newMMap.put(inter, origin);
        // update cAuto with the residual automaton that is left
        bddRep = Controller.bddManager.minus(bddRep, a);
      }
      if (bddRep != 0) {
        // if there's anything left of cAuto by the end, add it
        newMMap.put(bddRep, origin);
      }
      aToP = newMMap;

      timer.end(
          ExpressoLogger.LEVEL.DEBUG,
          "round: " + itr++ + ", origin: " + origin + ", number atoms: " + aToP.keySet().size());
    }

    initMaps(aToP);

    timer.logStatistic(ExpressoLogger.LEVEL.INFO, "round: " + itr + ", number atoms: " + _numAtoms);
    ExpressoLogger.popContext();
  }
}
