package application.experiments;

import propertycheckers.routing.RoutingChecker;
import bdd.BddManager;
import controlplane.network.CPExecutor;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import main.Controller;
import main.storage.Storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

public class ExpressoRunnerOnePrefix {
  public static void main(String[] args) {
    Prefix prefix = Prefix.of(args[0]);
    for (String network : Arrays.asList(args).subList(1, args.length)) {
      // storage
      Path net = Paths.get("networks").resolve(network);
      Controller.pushStorage(new Storage(net, null));

      // prefix
      HashSet<PrefixRange> prefixSpace = new HashSet<>();
      prefixSpace.add(PrefixRange.of(prefix));
      Controller.pushPrefixSpace(prefixSpace);

      // bdd manager
      Controller.pushBDDManager(new BddManager());

      // control plane
      long start = System.nanoTime();
      Controller.pushCPExecutor(new CPExecutor());
      Controller.cpExecutor.init();
      Controller.cpExecutor.execute();
      RoutingChecker.check(true, true);
      long end = System.nanoTime();
      System.out.printf("%s\t%s\t%s\n", network, prefix, ((end - start) / 1e9));
    }
  }
}
