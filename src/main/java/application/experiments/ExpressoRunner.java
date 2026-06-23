package application.experiments;

import propertycheckers.routing.RoutingChecker;
import bdd.BddManager;
import controlplane.network.CPExecutor;
import main.Configuration;
import main.Controller;
import main.storage.Storage;
import util.MemorizeUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Main entry point for Expresso. For arguments, use "comm", "asp", and "tp" to appoint symbolizing
 * communities, AS Path, and enabling consideration of traffic policies. Then appoint a list of
 * networks you want to check.
 */
public class ExpressoRunner {
  public static void main(String[] args) {
    List<String> argList = Arrays.asList(args);
    int skip = 0;

    if (argList.contains("comm")) {
      Configuration.SYMBOLIC_COMMUNITY = true;
      skip++;
    } else {
      Configuration.SYMBOLIC_COMMUNITY = false;
    }

    if (argList.contains("asp")) {
      Configuration.SYMBOLIC_AS_PATH = true;
      skip++;
    } else {
      Configuration.SYMBOLIC_AS_PATH = false;
    }

    if (argList.contains("tp")) {
      Configuration.ENABLE_TRAFFIC_POLICY = true;
      skip++;
    } else {
      Configuration.ENABLE_TRAFFIC_POLICY = false;
    }

    for (String network : Arrays.asList(args).subList(skip, args.length)) {
      Path net = Paths.get("networks").resolve(network);
      Controller.pushStorage(new Storage(net, null));
      Controller.pushBDDManager(new BddManager());

      long start = System.nanoTime();
      Controller.pushCPExecutor(new CPExecutor());
      Controller.cpExecutor.init();
      Controller.cpExecutor.execute();
      RoutingChecker.check(true, false);
      RoutingChecker.print();
      MemorizeUtil.printBgpRib();
      MemorizeUtil.printBgpTopology();
      long end = System.nanoTime();
      System.out.printf("%s\t%s\t", network, ((end - start) / 1e9));
    }
  }
}
