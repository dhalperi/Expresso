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

public class ExpressoFindDpLimit {
  static void appointIspNumber(Integer n) {
    if (n != null) {
      Controller.cpExecutor.bgpTopology.setIspStrategy(
          BgpTopology.IspStrategy.BOUNDED_NUMBER_OF_ISP, null, n, null);
    }
  }

  public static void main(String[] args) {
    Configuration.ENABLE_TRAFFIC_POLICY = true;
    Configuration.SYMBOLIC_COMMUNITY = false;
    Configuration.SYMBOLIC_AS_PATH = false;

    Configuration.DEFAULT_BDD_NT_SIZE = 700000000;

    String network = args[0];
    Integer nIsp = args.length >= 2 ? Integer.parseInt(args[1]) : null;

    Path net = Paths.get("networks").resolve(network);
    Controller.pushStorage(new Storage(net, null));

    Controller.pushBDDManager(new BddManager());

    Controller.pushCPExecutor(new CPExecutor());
    Controller.cpExecutor.init();
    appointIspNumber(nIsp);
    Controller.cpExecutor.execute();

    RoutingChecker.check(true, true);
    RoutingChecker.print(false, true);

    DPAnalyzer dpAnalyzer = new DPAnalyzer();
    dpAnalyzer.setApvMethod(true);
    dpAnalyzer.analyzeDataPlane();

    ForwardingChecker.check(dpAnalyzer, false, false, true, false, false);
    ForwardingChecker.ForwardingCheckResult result = ForwardingChecker.getResult();
    ForwardingChecker.print(false, true);

    System.out.printf(
        "%s\t%d\t%d\t%d\t%d\n",
        network,
        nIsp,
        RoutingChecker.numLeakedPrefixes,
        RoutingChecker.numHijackedPrefixes,
        result.hijacks.size());
  }
}
