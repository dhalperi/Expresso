package controlplane.process.ospf;

public class OspfEdge {
    OspfPeerConfig src;
    OspfPeerConfig dst;

    public OspfEdge(OspfPeerConfig src, OspfPeerConfig dst) {
        this.src = src;
        this.dst = dst;
    }

    public OspfPeerConfig getSrc() {
        return src;
    }

    public OspfPeerConfig getDst() {
        return dst;
    }
}
