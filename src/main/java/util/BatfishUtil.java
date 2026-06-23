package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.dataplane.ibdp.IncrementalDataPlanePlugin;
import org.batfish.main.Batfish;
import org.batfish.storage.FileBasedStorage;
import org.batfish.vendor.VendorConfiguration;

import static util.MemorizeUtil.deleteDir;

public class BatfishUtil {
  private static final Path NETWORKS_DIR = Paths.get("networks");

  /** Temporary folder to hold Batfish results. */
  private static final Path TEMP_DIR = NETWORKS_DIR.resolve(".temp");

  public static final String INPUT = "input";
  public static final String OUTPUT = "output";

  /**
   * Parse vendor-specific configurations (e.g., CISCO files) into vendor-independent configurations
   * using Batfish. Write the VIs and inferred topology into {@code configsPath}. This function
   * takes the raw configurations in the "networks/'network-name'/raw-configs" folder as input, and
   * outputs the JSON format VI configurations in the "networks/'network-name'/configs" folder.
   */
  public static void parseVendorSpecificConfigurations(String networkName) {
    try {
      long start = System.currentTimeMillis();
      // read the raw (vendor-specific) configurations
      Path rawConfigsDir = NETWORKS_DIR.resolve(networkName).resolve("raw-configs");
      Map<String, String> configTexts =
          getTextFromConfigs(
              Arrays.stream(Objects.requireNonNull(rawConfigsDir.toFile().list()))
                  .map(f -> rawConfigsDir.resolve(f).toString())
                  .collect(Collectors.toList()));

      // use batfish to parse the raw configurations
      String snapshotName = timestamp();
      Pair<Path, Batfish> pair =
          BatfishUtil.getBatfishFromTestrigText(
              networkName, snapshotName, configTexts, null, false);
      System.out.println(pair.getLeft());
      Batfish bf = pair.getValue();

      // write the VIs and inferred topology
      printVendorIndependentConfigurations(
          bf, NETWORKS_DIR.resolve(networkName).resolve("configs"));
      printTopology(bf, NETWORKS_DIR.resolve(networkName));

      // remove the temporary containers
      deleteDir(TEMP_DIR);

      System.out.println(
          "batfish parsing finished in " + (System.currentTimeMillis() - start) + "ms");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String timestamp() {
    Date date = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return simpleDateFormat.format(date);
  }

  /**
   * @param networkName network name
   * @param snapshotName snapshot name, default current time
   * @param configurationTexts map from hostnames to configuration files
   * @param layer1Topology path to the layer1 topology
   * @param useCache use cached vendor independent {@link Configuration}s or not.
   * @return a pair of (1) snapshot output folder, and (2) the batfish object
   */
  private static Pair<Path, Batfish> getBatfishFromTestrigText(
      String networkName,
      String snapshotName,
      Map<String, String> configurationTexts,
      @Nullable Path layer1Topology,
      boolean useCache) {
    initContainer(networkName, snapshotName, useCache);

    Settings settings = settings(networkName, snapshotName);

    Path snapshotDir = getSnapshotPath(networkName, snapshotName);
    writeTemporaryTestrigFiles(
        configurationTexts,
        snapshotDir.resolve(INPUT).resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR));
    if (layer1Topology != null && layer1Topology.toFile().exists()) {
      try {
        Files.copy(
            layer1Topology, snapshotDir.resolve(INPUT).resolve(BfConsts.RELPATH_L1_TOPOLOGY_PATH));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Batfish batfish =
        new Batfish(
            settings,
            makeTestrigCache(),
            makeDataPlaneCache(),
            makeEnvBgpCache(),
            makeVendorConfigurationCache(),
            new FileBasedStorage(settings.getStorageBase(), settings.getLogger()),
            null);

    // register data plane engine
    new IncrementalDataPlanePlugin().initialize(batfish);

    return Pair.of(snapshotDir, batfish);
  }

  private static void printVendorIndependentConfigurations(Batfish batfish, Path outputDir) {
    MemorizeUtil.createDirIfAbsentElseClean(outputDir.toFile());
    // we want to keep null values and empty lists, so we use the verbose mapper
    ObjectMapper mapper =
        BatfishObjectMapper.verboseMapper().enable(SerializationFeature.INDENT_OUTPUT);

    for (Map.Entry<String, Configuration> entry :
        batfish.loadConfigurations(batfish.getSnapshot()).entrySet()) {
      String name = entry.getKey();
      Configuration c = entry.getValue();
      String config;
      try {
        config = mapper.writeValueAsString(c);
        BufferedWriter bw =
            new BufferedWriter(new FileWriter(outputDir.resolve(name + ".json").toString()));
        bw.write(config);
        bw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void printTopology(Batfish batfish, Path outputDir) {
    Topology layer3Topology =
        batfish.getTopologyProvider().getRawLayer3Topology(batfish.getSnapshot());
    Map<String, Configuration> cMap = batfish.loadConfigurations(batfish.getSnapshot());
    layer3Topology
        .getEdges()
        .forEach(
            edge -> {
              Configuration c1 = cMap.get(edge.getHead().getHostname());
              Interface if1 = c1.getAllInterfaces().get(edge.getHead().getInterface());
              Configuration c2 = cMap.get(edge.getTail().getHostname());
              Interface if2 = c2.getAllInterfaces().get(edge.getTail().getInterface());
              if (if1.getChannelGroup() != null || if2.getChannelGroup() != null) {
                System.out.println(edge);
              }
            });
    List<String> lines =
        layer3Topology.getEdges().stream()
            .map(
                edge ->
                    String.join(
                        "\t",
                        edge.getTail().getHostname(),
                        edge.getTail().getInterface(),
                        edge.getHead().getHostname(),
                        edge.getHead().getInterface()))
            .sorted()
            .collect(Collectors.toList());
    try (BufferedWriter bw =
        new BufferedWriter(new FileWriter(outputDir.resolve("topology.txt").toString()))) {
      bw.write(String.join("\n", lines));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Helper function: get a batfish {@link Settings}. */
  private static Settings settings(String networkName, String snapshotName) {
    Settings settings = new Settings(new String[] {});

    settings.setLogger(new BatfishLogger("debug", false));
    settings.setDisableUnrecognized(true);
    settings.setHaltOnConvertError(true);
    settings.setHaltOnParseError(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);
    settings.setVerboseParse(true);

    settings.setStorageBase(TEMP_DIR);

    settings.setContainer(networkName); // networkId
    settings.setTestrig(snapshotName); // snapshotId
    settings.setSnapshotName(snapshotName); // snapshotName

    return settings;
  }

  private static Path getSnapshotPath(String networkName, String snapshotName) {
    return TEMP_DIR
        .resolve("networks")
        .resolve(networkName)
        .resolve("snapshots")
        .resolve(snapshotName);
  }

  private static void initContainer(String networkName, String snapshotName, boolean useCache) {
    Path snapshotDir = getSnapshotPath(networkName, snapshotName);
    Path networkBlobs = snapshotDir.getParent().getParent().resolve("blobs");
    try {
      if (!useCache) FileUtils.deleteDirectory(networkBlobs.toFile());
      if (snapshotDir.toFile().exists()) FileUtils.deleteDirectory(snapshotDir.toFile());
      Files.createDirectories(snapshotDir.resolve(INPUT));
      Files.createDirectories(snapshotDir.resolve(OUTPUT));
    } catch (IOException ignored) {
    }
  }

  /** Helper function: read all raw configs. */
  private static Map<String, String> getTextFromConfigs(List<String> configurationNames)
      throws IOException {
    SortedMap<String, String> configurationTextMap = new TreeMap<>();
    for (String configName : configurationNames) {
      byte[] encoded = Files.readAllBytes(Paths.get(configName));
      String configurationText = new String(encoded, Charset.defaultCharset());
      configurationTextMap.put(new File(configName).getName(), configurationText);
    }
    return configurationTextMap;
  }

  private static void writeTemporaryTestrigFiles(
      Map<String, String> filesText, Path outputDirectory) {
    if (filesText != null) {
      boolean b = outputDirectory.toFile().mkdirs();
      if (!b) System.err.println("mkDirs() failed for" + outputDirectory);
      filesText.forEach(
          (filename, text) -> CommonUtil.writeFile(outputDirectory.resolve(filename), text));
    }
  }

  /** Helper function: initialize a cache for parsed VIs. */
  private static Cache<NetworkSnapshot, SortedMap<String, Configuration>> makeTestrigCache() {
    return CacheBuilder.newBuilder().softValues().maximumSize(5).build();
  }

  /** Helper function: initialize a cache for generated data planes. */
  private static Cache<NetworkSnapshot, DataPlane> makeDataPlaneCache() {
    return CacheBuilder.newBuilder().softValues().maximumSize(2).build();
  }

  /** Helper function: initialize a cache for external BGP advertisements. */
  private static Map<NetworkSnapshot, SortedMap<String, BgpAdvertisementsByVrf>> makeEnvBgpCache() {
    return Collections.synchronizedMap(new LRUMap<>(4));
  }

  /**
   * Helper function: initialize a cache for vendor specific configurations (i.e., {@link
   * VendorConfiguration}).
   */
  private static Cache<NetworkSnapshot, Map<String, VendorConfiguration>>
      makeVendorConfigurationCache() {
    return CacheBuilder.newBuilder().softValues().maximumSize(2).build();
  }

  public static void main(String[] args) {
    parseVendorSpecificConfigurations(args[0]);
  }
}
