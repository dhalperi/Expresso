package application.experiments;

import propertycheckers.forwarding.ForwardingChecker;
import propertycheckers.routing.RoutingChecker;
import bdd.BddManager;
import controlplane.network.CPExecutor;
import controlplane.process.bgp.BgpTopology;
import dataplane.analysis.DPAnalyzer;
import main.Configuration;
import main.Controller;
import main.storage.Storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ExpressoRunnerWithDP {
  public static void main(String[] args) {
    Configuration.SYMBOLIC_COMMUNITY = args[0].equals("true");
    Configuration.SYMBOLIC_AS_PATH = args[1].equals("true");
    Configuration.ENABLE_TRAFFIC_POLICY = args[2].equals("true");

    boolean randomIsp = args[3].equals("true");
    boolean apv = args[4].equals("true");
    int nIsp = Integer.parseInt(args[5]);

    for (String network : Arrays.asList(args).subList(6, args.length)) {
      Path net = Paths.get("networks").resolve(network);
      Controller.pushStorage(new Storage(net, null));
      Controller.pushBDDManager(new BddManager());

      long start, end;
      double src, rpa, spf, fpa, total;

      start = System.nanoTime();
      Controller.pushCPExecutor(new CPExecutor());
      Controller.cpExecutor.init();
      Controller.cpExecutor.bgpTopology.setIspStrategy(
          randomIsp
              ? BgpTopology.IspStrategy.BOUNDED_NUMBER_OF_ISP_RANDOM
              : BgpTopology.IspStrategy.BOUNDED_NUMBER_OF_ISP,
          null,
          nIsp,
          null);
      Controller.cpExecutor.execute();
      end = System.nanoTime();
      src = (end - start) / 1e9;

      start = System.nanoTime();
      RoutingChecker.check();
      end = System.nanoTime();
      rpa = (end - start) / 1e9;

      start = System.nanoTime();
      DPAnalyzer dpAnalyzer = new DPAnalyzer();
      dpAnalyzer.setApvMethod(apv);
      dpAnalyzer.analyzeDataPlane();
      end = System.nanoTime();
      spf = (end - start) / 1e9;

      start = System.nanoTime();
      ForwardingChecker.check(dpAnalyzer);
      end = System.nanoTime();
      fpa = (end - start) / 1e9;

      total = src + rpa + spf + fpa;
      System.out.printf("%s\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", network, src, rpa, spf, fpa, total);

      //      RoutingChecker.print();
      //      ForwardingChecker.print();
      //      MemorizeUtil.printBgpRib();
      //      MemorizeUtil.printRouterRib("sh-bx-dpr");
    }
  }
}
