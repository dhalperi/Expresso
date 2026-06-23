package application.experiments;

import bdd.BddManager;
import controlplane.network.CPExecutor;
import main.Configuration;
import main.Controller;
import main.storage.Storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Evaluation for Figure 7 in our paper. This evaluation compares the performance of Expresso by
 * using either automaton or atomic predicates to represent symbolic communities and symbolic AS
 * Path.
 *
 * <p>The main function takes three arguments as input: <br>
 * (1) The first argument should be "comm" or "ap", stands for symbolizing communities or AS Path,
 * respectively. <br>
 * (2) The second argument should be "true" or "false", stands for using atomic predicates or
 * automatons, respectively. <br>
 * (3) The third argument should be the name of a network.
 */
public class ExpressoAutoOrAp {
  public static void main(String[] args) {
    long start = System.nanoTime();

    boolean comm = args[0].equals("comm");
    boolean ap = args[1].equals("true");

    Configuration.SYMBOLIC_COMMUNITY = comm;
    Configuration.SYMBOLIC_COMMUNITY_AP = ap;

    Configuration.SYMBOLIC_AS_PATH = !comm;
    Configuration.SYMBOLIC_AS_PATH_AP = ap;

    String network = args[2];

    Path net = Paths.get("networks").resolve(network);
    Controller.pushStorage(new Storage(net, null));

    Controller.pushBDDManager(new BddManager());

    Controller.pushCPExecutor(new CPExecutor());
    Controller.cpExecutor.init();
    Controller.cpExecutor.execute();

    System.out.printf(
        "%s\t%s\t%s\t%f\t",
        network, comm ? "comm" : "asp", ap ? "ap" : "auto", (System.nanoTime() - start) / 1e9);
  }
}
