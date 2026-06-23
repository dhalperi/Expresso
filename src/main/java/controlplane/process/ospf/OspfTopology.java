package controlplane.process.ospf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import controlplane.network.Interface;
import controlplane.network.L3Topology;
import controlplane.network.VirtualRouter;
import main.ExpressoLogger;

import java.util.List;
import java.util.stream.Collectors;

public class OspfTopology {
    // todo: design it better
    Table<OspfRoutingProcess, OspfRoutingProcess, OspfEdge> edges;

    public OspfTopology() {
        this.edges = HashBasedTable.create();
    }

    public void addEdge(OspfPeerConfig n1, OspfPeerConfig n2) {
        OspfEdge e1 = new OspfEdge(n1, n2);
        OspfEdge e2 = new OspfEdge(n2, n1);
        edges.put(n1.process, n2.process, e1);
        edges.put(n2.process, n1.process, e2);
    }

    public void build(L3Topology l3Topology) {
        l3Topology.getL3Edges().forEach(l3edge -> {
            Interface intf1 = l3Topology.getInterfaces().get(l3edge.getSrc());
            Interface intf2 = l3Topology.getInterfaces().get(l3edge.getDst());
            if (intf1 == null || intf2 == null) {
                ExpressoLogger.log(
                        ExpressoLogger.LEVEL.ERROR,
                        String.format(
                                "l3 edge %s has nonexistent interface, %s, %s",
                                l3edge,
                                intf1,
                                intf2)
                );
            }
            if (canEstablishOspfSession(intf1, intf2)) {
                VirtualRouter vrf1 = intf1.getVrf();
                VirtualRouter vrf2 = intf2.getVrf();
                if (vrf1.getOspfProcesses() != null && vrf2.getOspfProcesses() != null) {
                    // find the ospf process that intf1 is active for
                    List<OspfRoutingProcess> list1 = vrf1.getOspfProcesses().values()
                            .stream()
                            .filter(p -> p.ownInterface(intf1))
                            .collect(Collectors.toList());
                    // find the ospf process that intf2 is active for
                    List<OspfRoutingProcess> list2 = vrf2.getOspfProcesses().values()
                            .stream()
                            .filter(p -> p.ownInterface(intf2))
                            .collect(Collectors.toList());
                    assert list1.size() == 1;
                    assert list2.size() == 1;
                    for (OspfRoutingProcess process1 : list1) {
                        for (OspfRoutingProcess process2 : list2) {
                            OspfPeerConfig c1 = new OspfPeerConfig(process1, intf1.getOspfIntfSetting(), intf1);
                            OspfPeerConfig c2 = new OspfPeerConfig(process2, intf2.getOspfIntfSetting(), intf2);
                            addEdge(c1, c2);
                        }
                    }
                }
            }
        });
    }

    public boolean canEstablishOspfSession(Interface intf1, Interface intf2) {
        return intf1 != null
                && intf2 != null
                && intf1.getOspfIntfSetting().ospfEnabled
                && intf2.getOspfIntfSetting().ospfEnabled
                && intf1.getOspfIntfSetting().ospfArea == intf2.getOspfIntfSetting().ospfArea;
    }
}
