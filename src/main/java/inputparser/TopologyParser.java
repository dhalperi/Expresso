package inputparser;

import controlplane.network.InterfaceName;
import controlplane.network.L3Edge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

public class TopologyParser {
    public static HashSet<L3Edge> parseL3Edges(Path path) {
        HashSet<L3Edge> l3Edges = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
            String oneLine;
            while ((oneLine = reader.readLine()) != null) {
                String[] parts = oneLine.split("\t");
                InterfaceName p1 = new InterfaceName(parts[0], parts[1]);
                InterfaceName p2 = new InterfaceName(parts[2], parts[3]);
                L3Edge l3Edge = L3Edge.of(p1, p2);
                l3Edges.add(l3Edge);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return l3Edges;
    }
}
