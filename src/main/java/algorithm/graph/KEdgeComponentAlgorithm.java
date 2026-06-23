package algorithm.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class KEdgeComponentAlgorithm<V, E> {
  protected Graph<V, E> graph;

  public KEdgeComponentAlgorithm(Graph<V, E> graph) {
    this.graph = Objects.requireNonNull(graph, "Graph can not be null");
  }

  public Collection<Graph<V, E>> getKEdgeComponent(int k) {
    if (k < 1) {
      throw new IllegalArgumentException("k cannot be less than 1");
    }
    if (graph.getType().isAllowingMultipleEdges()) {
      return new EdgeComponentAuxGraph<>(graph)
          .getKEdgeComponents(k).stream()
              .map(vs -> new AsSubgraph<>(graph, vs))
              .collect(Collectors.toSet());
    }
    if (graph.getType().isDirected()) {
      if (k == 1) {
        // return strongly_connected_components(G)
        return new GabowStrongConnectivityInspector<>(graph).getStronglyConnectedComponents();
      } else {
        return new EdgeComponentAuxGraph<>(graph)
            .getKEdgeComponents(k).stream()
                .map(vs -> new AsSubgraph<>(graph, vs))
                .collect(Collectors.toSet());
      }
    } else {
      if (k == 1) {
        // return connected_components(G)
        return new BiconnectivityInspector<>(graph).getConnectedComponents();
      } else if (k == 2) {
        graph.removeAllEdges(new BiconnectivityInspector<>(graph).getBridges());
        return new BiconnectivityInspector<>(graph).getConnectedComponents();
      } else {
        return new EdgeComponentAuxGraph<>(graph)
            .getKEdgeComponents(k).stream()
                .map(vs -> new AsSubgraph<>(graph, vs))
                .collect(Collectors.toSet());
      }
    }
  }
}
