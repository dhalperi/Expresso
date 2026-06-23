package controlplane.process.ospf;

import controlplane.network.Interface;

public class OspfPeerConfig {
    OspfRoutingProcess process;
    OspfIntfSetting intfSetting;
    Interface intf;

    public OspfPeerConfig(OspfRoutingProcess process, OspfIntfSetting intfSetting, Interface intf) {
        this.process = process;
        this.intfSetting = intfSetting;
        this.intf = intf;
    }
}
