package main.storage;

import main.Configuration;
import main.ExpressoLogger;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import util.TimeUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Storage {
  public static String TODAY = TimeUtil.getDate();

  public Input input;
  public Output output;

  public Storage(Path inputBase, Path outputBase) {
    input = new Input.Builder().setInputBase(inputBase).build();

    List<String> configOptions = new LinkedList<>();
    if (Configuration.ENABLE_TRAFFIC_POLICY) configOptions.add("traffic_policy");
    if (Configuration.SYMBOLIC_COMMUNITY) configOptions.add("symbolic_comm");
    if (Configuration.SYMBOLIC_AS_PATH) configOptions.add("symbolic_asp");
    String configSuffix = configOptions.isEmpty() ? "none" : String.join("-", configOptions);

    output =
        new Output.Builder()
            .setOutputBase(
                outputBase != null
                    ? outputBase
                    : inputBase.resolve("out").resolve(TODAY).resolve(configSuffix))
            .build();
  }

  public Storage(Input input, Output output) {
    this.input = input;
    this.output = output;
  }

  public void init() {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(output._log.toFile(), true))) {
      bw.append("\n");
    } catch (IOException ignored) {

    }

    PatternLayout layout = new PatternLayout();
    layout.setConversionPattern("%p %d{yyyy/MM/dd HH:mm:ss,SSS}  %m%n");

    FileAppender appender = new FileAppender();
    appender.setEncoding("UTF-8");
    appender.setThreshold(Level.INFO);
    appender.setAppend(true);
    appender.setImmediateFlush(true);
    appender.setLayout(layout);
    appender.setFile(output._log.toString());
    appender.activateOptions();

    ExpressoLogger.setAppender(appender);
  }
}
