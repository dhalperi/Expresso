package util;

import controlplane.network.Router;
import controlplane.network.VirtualRouter;
import controlplane.process.bgp.BgpPeerConfig;
import controlplane.process.bgp.BgpRoutingProcess;
import controlplane.process.bgp.BgpSessionProperties;
import controlplane.process.bgp.ISP;
import main.Controller;
import main.ExpressoLogger;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class MemorizeUtil {

  public static void createDirIfAbsent(File dir) {
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        ExpressoLogger.log(ExpressoLogger.LEVEL.ERROR, "Cannot create dir " + dir);
        System.exit(1);
      }
    }
  }

  public static void createDirIfAbsentElseClean(File dir) {
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        ExpressoLogger.log(ExpressoLogger.LEVEL.ERROR, "Cannot create dir " + dir);
        System.exit(1);
      }
    } else {
      try {
        FileUtils.cleanDirectory(dir);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void creatFileIfAbsent(File file) throws IOException {
    if (!file.exists()) {
      createDirIfAbsent(file.getParentFile());
      if (!file.createNewFile()) {
        ExpressoLogger.log(ExpressoLogger.LEVEL.ERROR, "Cannot create file " + file);
        System.exit(1);
      }
    }
  }

  public static void deleteDir(Path dir) throws IOException {
    if (Files.exists(dir) && dir.toFile().isDirectory()) {
      for (String child : Objects.requireNonNull(dir.toFile().list())) {
        deleteDir(dir.resolve(child));
      }
    }
    Files.delete(dir);
  }

  public static void printRib() {
    createDirIfAbsentElseClean(Controller.storage.output._rib.toFile());
    Controller.cpExecutor.l3Topology.getNodes().keySet().forEach(MemorizeUtil::printRouterRib);
    Controller.cpExecutor.bgpTopology.getISPs().forEach(MemorizeUtil::printIspRib);
  }

  public static void printRouterRib(String routerName) {
    createDirIfAbsent(Controller.storage.output._rib.toFile());

    Router router = Controller.cpExecutor.l3Topology.getNodes().get(routerName);
    Path ribFile = Controller.storage.output._rib.resolve(routerName + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ribFile.toFile()))) {
      for (Map.Entry<String, VirtualRouter> entry2 : router.getVirtualRouters().entrySet()) {
        String vrfName = entry2.getKey();
        VirtualRouter vrf = entry2.getValue();
        bw.write(String.format("[VRF %s]\n", vrfName));
        bw.write(vrf.getGlobalRib().toString());
        bw.write("\n\n\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printIspRib(ISP isp) {
    if (isp.getRib().getAllRoutes().isEmpty()) return;
    createDirIfAbsent(Controller.storage.output._rib.toFile());

    Path ribFile = Controller.storage.output._rib.resolve(isp.getName() + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ribFile.toFile()))) {
      bw.write(isp.getRib().toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printFib() {
    createDirIfAbsentElseClean(Controller.storage.output._fib.toFile());
    Controller.cpExecutor.l3Topology.getNodes().keySet().forEach(MemorizeUtil::printFib);
  }

  public static void printFib(String routerName) {
    createDirIfAbsent(Controller.storage.output._fib.toFile());

    Router router = Controller.cpExecutor.l3Topology.getNodes().get(routerName);
    Path fibFile = Controller.storage.output._fib.resolve(routerName + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fibFile.toFile()))) {
      for (Map.Entry<String, VirtualRouter> entry2 : router.getVirtualRouters().entrySet()) {
        String vrfName = entry2.getKey();
        VirtualRouter vrf = entry2.getValue();
        bw.write(String.format("[VRF %s]\n", vrfName));
        bw.write(vrf.getFib().toString());
        bw.write("\n\n\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printIsisRib() {
    Path isis = Controller.storage.output._isis;
    createDirIfAbsentElseClean(isis.toFile());
    Controller.cpExecutor.l3Topology.getNodes().keySet().forEach(MemorizeUtil::printRouterIsisRib);
  }

  public static void printRouterIsisRib(String routerName) {
    Path isis = Controller.storage.output._isis;
    createDirIfAbsent(isis.toFile());

    Router router = Controller.cpExecutor.l3Topology.getNodes().get(routerName);

    boolean has =
        router.getVirtualRouters().values().stream()
            .anyMatch(
                vrf ->
                    vrf.getIsisProcess() != null
                        && !vrf.getIsisProcess().getRib().getAllRoutes().isEmpty());
    if (!has) return;

    Path ribFile = isis.resolve(routerName + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ribFile.toFile()))) {
      for (Map.Entry<String, VirtualRouter> entry2 : router.getVirtualRouters().entrySet()) {
        String vrfName = entry2.getKey();
        VirtualRouter vrf = entry2.getValue();
        bw.write(String.format("[VRF %s]\n", vrfName));
        if (vrf.getIsisProcess() != null) {
          bw.write(vrf.getIsisProcess().getRib().toString());
        }
        bw.write("\n\n\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printBgpRib() {
    Path bgp = Controller.storage.output._bgp;
    createDirIfAbsentElseClean(bgp.toFile());
    Controller.cpExecutor.l3Topology.getNodes().keySet().forEach(MemorizeUtil::printRouterBgpRib);
    Controller.cpExecutor.bgpTopology.getISPs().forEach(MemorizeUtil::printIspBgpRib);
  }

  public static void printRouterBgpRib(String routerName) {
    Path bgp = Controller.storage.output._bgp;
    createDirIfAbsent(bgp.toFile());

    Router router = Controller.cpExecutor.l3Topology.getNodes().get(routerName);

    boolean has =
        router.getVirtualRouters().values().stream()
            .anyMatch(
                vrf ->
                    vrf.getBgpProcess() != null
                        && !vrf.getBgpProcess().getRib().getAllRoutes().isEmpty());
    if (!has) return;

    Path ribFile = bgp.resolve(routerName + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ribFile.toFile()))) {
      for (Map.Entry<String, VirtualRouter> entry2 : router.getVirtualRouters().entrySet()) {
        String vrfName = entry2.getKey();
        VirtualRouter vrf = entry2.getValue();
        bw.write(String.format("[VRF %s]\n", vrfName));
        if (vrf.getBgpProcess() != null) {
          bw.write(vrf.getBgpProcess().getRib().toString());
        }
        bw.write("\n\n\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printIspBgpRib(ISP isp) {
    Path bgp = Controller.storage.output._bgp;
    createDirIfAbsent(bgp.toFile());

    if (isp.getRib().getAllRoutes().isEmpty()) return;

    Path ribFile = bgp.resolve(isp.getName() + ".txt");
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ribFile.toFile()))) {
      bw.write(isp.getRib().toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printBgpTopology() {
    try {
      Path topo = Controller.storage.output._topo;
      createDirIfAbsent(topo.toFile());
      Path bgpTopo = topo.resolve("expresso_bgp_topology.txt");
      BufferedWriter bw = new BufferedWriter(new FileWriter(bgpTopo.toFile()));
      for (Map.Entry<BgpRoutingProcess, Map<BgpPeerConfig, BgpSessionProperties>> entry :
          Controller.cpExecutor.bgpTopology.getEdges().rowMap().entrySet()) {
        for (Map.Entry<BgpPeerConfig, BgpSessionProperties> entry1 : entry.getValue().entrySet()) {
          BgpSessionProperties session = entry1.getValue();
          bw.write(
              String.format(
                  "%s\t%s\t%s\t%s\n",
                  session.getLocal().getProcess().getVrf().getFullName(),
                  session.getLocal().getLocalIp().toString(),
                  session.getRemote().getProcess().getVrf().getFullName(),
                  session.getRemote().getLocalIp().toString()));
        }
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {}
}
