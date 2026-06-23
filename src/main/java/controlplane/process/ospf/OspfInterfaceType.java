package controlplane.process.ospf;

public enum OspfInterfaceType {
    /** A network that joins a single pair of routers */
    P2P,
    /**
     * A network supporting more than 2 routers w/ capability to simultaneously address all attached
     * routers
     */
    BROADCAST,

    // The following are non-broadcast network modes, which support more than 2 routers, but have no
    // broadcast capability
    /** A non-broadcast network mode that simulates the operation of a broadcast network */
    NON_BROADCAST_MULTI_ACCESS,
    /**
     * A non-broadcast network mode that treats the network like a collection of point-to-point links
     */
    POINT_TO_MULTIPOINT
}
