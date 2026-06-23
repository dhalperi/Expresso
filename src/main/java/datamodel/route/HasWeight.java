package datamodel.route;

/**
 * On Cisco devices, bgp routes has an attribute named weight.
 * However, on Huawei devices, it is called preferred value.
 * The preferred value of a route indicates the weight of the route in BGP routing.
 * The preferred value is not a standard RFC-defined attribute and is valid only on local devices.
 * The preferred value is inapplicable to export policies of BGP.
 */
public interface HasWeight {
    int getWeight();

    void setWeight(int weight);
}
