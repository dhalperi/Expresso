package algorithm.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.flow.GusfieldGomoryHuCutTree;
import org.jgrapht.graph.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Reference: Wang, Tianhao, et al. (2015) A simple algorithm for finding all k-edge-connected
 * components. http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0136264
 */
public class EdgeComponentAuxGraph<V, E> extends SimpleWeightedGraph<V, DefaultWeightedEdge> {
  // The auxiliary alg.graph is a weighted undirected tree
  private static final long serialVersionUID = 6135610474578704120L;

  Graph<V, DefaultWeightedEdge> originalG;
  GusfieldGomoryHuCutTree<V, DefaultWeightedEdge> minCutTree;

  public EdgeComponentAuxGraph(Graph<V, E> graph) {
    super(DefaultWeightedEdge.class);
    if (graph.getType().isDirected()) {
      //            originalG = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
      originalG = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
    } else {
      //            originalG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
      originalG = new WeightedMultigraph<>(DefaultWeightedEdge.class);
    }
    graph
        .vertexSet()
        .forEach(
            v -> {
              originalG.addVertex(v);
              addVertex(v);
            });
    graph
        .edgeSet()
        .forEach(
            e -> {
              DefaultWeightedEdge e1 =
                  originalG.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e));
              originalG.setEdgeWeight(e1, 1);
            });
    construct();
  }

  private void construct() {
    if (originalG.vertexSet().size() > 0) {
      minCutTree = new GusfieldGomoryHuCutTree<>(originalG);
      V source = originalG.vertexSet().iterator().next();
      Set<V> avail = originalG.vertexSet();
      construct_rec(source, avail);
    }
  }

  private void construct_rec(V source, Set<V> avail) {
    // terminate once the flow has been compute to every node
    if (avail.size() == 1 && avail.contains(source)) {
      return;
    }
    // pick an arbitrary node as the sink
    V sink = null;
    for (V v : avail) {
      if (!v.equals(source)) {
        sink = v;
        break;
      }
    }
    // find the minimum cut and its weight
    double value = minCutTree.calculateMinCut(source, sink);
    Set<V> S = minCutTree.getSourcePartition();
    Set<V> T = minCutTree.getSinkPartition();
    if (originalG.getType().isDirected()) {
      // check if the reverse direction has a smaller cut
      double value1 = minCutTree.calculateMinCut(sink, source);
      Set<V> S1 = minCutTree.getSourcePartition();
      Set<V> T1 = minCutTree.getSinkPartition();
      if (value1 < value) {
        value = value1;
        S = S1;
        T = T1;
      }
    }
    // add edge with weight of cut to the aux alg.graph
    DefaultWeightedEdge e = addEdge(source, sink);
    setEdgeWeight(e, value);
    // recursively call until all but one node is used
    S.retainAll(avail);
    construct_rec(source, S);
    T.retainAll(avail);
    construct_rec(sink, T);
  }

  public Set<Set<V>> getKEdgeComponents(int k) {
    if (k < 1) {
      throw new IllegalArgumentException("k cannot be less than 1");
    } else {
      SimpleWeightedGraph<V, DefaultWeightedEdge> g =
          new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
      vertexSet().forEach(g::addVertex);
      edgeSet().stream()
          .filter(e -> getEdgeWeight(e) >= k)
          .forEach(e -> g.addEdge(getEdgeSource(e), getEdgeTarget(e)));
      return new HashSet<>(new ConnectivityInspector<>(g).connectedSets());
    }
  }
}
