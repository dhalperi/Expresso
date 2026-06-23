package bdd;

import jdd.bdd.BDD;

public abstract class BddWrapper {
    BDD bdd;
    BddManager manager;

    public BddWrapper(BddManager manager) {
        this.manager = manager;
        this.bdd = manager.bdd;
    }

    /**
     * Fill the array from the tail to the head.
     *
     * @param vars the int array to put bdd variables in
     * @param bits number of variables
     */
    void DeclareVars(int[] vars, int bits) {
        for (int i = bits - 1; i >= 0; i--) {
            vars[i] = bdd.createVar();
        }
    }
}
