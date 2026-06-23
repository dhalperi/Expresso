package inputparser.bfvi;

import datamodel.ipv4.Ip;
import main.Controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PeerIpParser {
  public static TreeSet<Ip> parsePeerIps(Path path) {
    if (path == null) {
      path = Controller.storage.input._inputBase.resolve("bagpipe-peers.txt");
    }
    TreeSet<Ip> ips = new TreeSet<>();
    if (path.toFile().exists()) {
      try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
        List<String> lines = br.lines().collect(Collectors.toList());
        for (String line : lines) {
          String[] tmp = line.substring(line.indexOf("(") + 1, line.indexOf(")")).split(" ");
          Ip ip = Ip.parse(String.format("%s.%s.%s.%s", tmp[1], tmp[2], tmp[3], tmp[4]));
          ips.add(ip);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return ips;
  }
}
