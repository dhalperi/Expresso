package application.experiments;

import propertycheckers.routing.RoutingChecker;
import bdd.BddManager;
import controlplane.network.CPExecutor;
import controlplane.process.bgp.BgpTopology;
import main.Configuration;
import main.Controller;
import main.storage.Storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Evaluation for Figure 6(a) and 8(a) in our paper. This evaluation runs Expresso on the CSP's WAN,
 * with bounded number of external neighbors.
 *
 * <p>The main function takes three boolean values, one integer, and one string as input.
 * Specifically, the first three boolean values indicate whether symbolizing communities,
 * symbolizing AS Path, and enabling consideration of traffic policies sequentially; the integer
 * indicates the number of external neighbors to activate; and the string indicates the network
 * name.
 */
public class ExpressoRunnerBoundedNumIsp {
  public static void main(String[] args) {
    Configuration.SYMBOLIC_COMMUNITY = args[0].equals("true");
    Configuration.SYMBOLIC_AS_PATH = args[1].equals("true");
    Configuration.ENABLE_TRAFFIC_POLICY = args[2].equals("true");

    int nIsp = Integer.parseInt(args[3]);
    String network = args[4];
    Path net = Paths.get("networks").resolve(network);
    Controller.pushStorage(new Storage(net, null));
    Controller.pushBDDManager(new BddManager());

    long start = System.nanoTime();
    Controller.pushCPExecutor(new CPExecutor());
    Controller.cpExecutor.init();
    Controller.cpExecutor.bgpTopology.setIspStrategy(
        BgpTopology.IspStrategy.BOUNDED_NUMBER_OF_ISP, null, nIsp, null);
    Controller.cpExecutor.execute();
    RoutingChecker.check(true, false);
    long end = System.nanoTime();
    System.out.printf(
        "%s\t%d\t%s\t",
        network, Controller.cpExecutor.bgpTopology.getISPs().size(), ((end - start) / 1e9));

    Controller.popBDDManager();
    Controller.popCPExecutor();
    Controller.popStorage();
  }
}
