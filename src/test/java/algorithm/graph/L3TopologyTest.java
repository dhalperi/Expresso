package algorithm.graph;

import controlplane.network.L3Edge;
import org.jgrapht.Graph;
import org.jgrapht.graph.Multigraph;
import inputparser.ConfigurationParser;
import controlplane.network.Router;
import org.junit.Test;
import inputparser.TopologyParser;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class L3TopologyTest
{
    public static class Topology {

        Map<String, Router> _vertex;
        Set<L3Edge> _edges;

        Graph<Router, L3Edge> _graph;

        public Topology(Map<String, Router> vertex, Set<L3Edge> edges) {
            _vertex = vertex;
            _edges = edges;
            buildGraph();
        }

        private void buildGraph() {
            _graph = new Multigraph<>(L3Edge.class);
            _vertex.values().forEach(v -> _graph.addVertex(v));
            _edges.forEach(e -> {
                Router v1 = _vertex.get(e.getSrc().getRouterName());
                Router v2 = _vertex.get(e.getDst().getRouterName());
                _graph.addEdge(v1, v2, e);
            });
        }

        public Set<Set<Router>> getKEdgeComponent(int k) {
            KEdgeComponentAlgorithm<Router, L3Edge> alg = new KEdgeComponentAlgorithm<>(_graph);
            return alg.getKEdgeComponent(k).stream().map(Graph::vertexSet).collect(Collectors.toSet());
        }
    }

    @Test
    public void getKEdgeComponent()
    {
//        /* bics */
//        TreeMap<String, Router> routers1 = ConfigurationParser.parseDirectory(Paths.get("./networks/bgp/bics/configs"));
//        HashSet<Link> links1 = TopologyParser.parseLinks(Paths.get("./networks/bgp/bics/topology.txt"));
//        Topology topology1 = new Topology(routers1, links1);
//        assertEquals(1, topology1.getKEdgeComponent(1).size());
//        assertEquals(23, topology1.getKEdgeComponent(2).size());
//        assertEquals(34, topology1.getKEdgeComponent(3).size());
//        assertEquals(43, topology1.getKEdgeComponent(4).size());

//        /* columbus */
//        TreeMap<String, Router> routers2 = ConfigurationParser.parseDirectory(Paths.get("./networks/bgp/columbus/configs"));
//        HashSet<Link> links2 = TopologyParser.parseLinks(Paths.get("./networks/bgp/columbus/topology.txt"));
//        Topology topology2 = new Topology(routers2, links2);
//        assertEquals(1, topology2.getKEdgeComponent(1).size());
//        assertEquals(30, topology2.getKEdgeComponent(2).size());
//        assertEquals(65, topology2.getKEdgeComponent(3).size());
//        assertEquals(82, topology2.getKEdgeComponent(4).size());

        /* us carrier */
        ConfigurationParser parser = new ConfigurationParser();
        parser.parse(Paths.get("./networks/bgp/uscarrier/configs"));
        TreeMap<String, Router> routers3 = parser.getRouters();
        HashSet<L3Edge> links3 = new TopologyParser().parseL3Edges(Paths.get("./networks/bgp/uscarrier/topology.txt"));
        Topology topology3 = new Topology(routers3, links3);
        assertEquals(1, topology3.getKEdgeComponent(1).size());
        assertEquals(48, topology3.getKEdgeComponent(2).size());
        assertEquals(142, topology3.getKEdgeComponent(3).size());
        assertEquals(172, topology3.getKEdgeComponent(4).size());
    }
}
