package application.experiments;

import propertycheckers.routing.BlockToExternalChecker;
import bdd.BddManager;
import controlplane.network.CPExecutor;
import main.Configuration;
import main.storage.Storage;

import java.nio.file.Path;
import java.nio.file.Paths;

import static main.Controller.cpExecutor;
import static main.Controller.pushBDDManager;
import static main.Controller.pushCPExecutor;
import static main.Controller.pushStorage;

/**
 * Evaluation for Table 4 in our paper. This evaluation runs Expresso to check BlockToExternal on
 * the Internet2.
 *
 * <p>The main function takes three boolean values as input, indicating whether symbolizing
 * communities, symbolizing AS Path, and enabling consideration of traffic policies sequentially.
 */
public class ExpressoBlockToExternal {
  public static void main(String... args) {
    long start = System.nanoTime();
    String networkName = "internet2";

    Configuration.SYMBOLIC_COMMUNITY = args[0].equals("true");
    Configuration.SYMBOLIC_AS_PATH = args[1].equals("true");
    Configuration.ENABLE_TRAFFIC_POLICY = args[2].equals("true");

    Path net = Paths.get("networks").resolve(networkName);
    pushStorage(new Storage(net, null));

    pushBDDManager(new BddManager());
    pushCPExecutor(new CPExecutor());
    cpExecutor.init();
    cpExecutor.execute();

    long end = System.nanoTime();
    System.out.printf("%s\t%s\t", networkName, ((end - start) / 1e9));

    BlockToExternalChecker.check(true);
  }
}
