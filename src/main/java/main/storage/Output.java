package main.storage;

import main.Configuration;
import main.Controller;

import java.nio.file.Path;
import java.util.Objects;

public class Output {
  public final Path _outputBase;
  public final Path _log;
  public final Path _fib;
  public final Path _rib;
  public final Path _ospf;
  public final Path _isis;
  public final Path _bgp;
  public final Path _topo;

  public Output(
      Path outputBase, Path log, Path fib, Path rib, Path ospf, Path isis, Path bgp, Path topo) {
    _outputBase = outputBase;
    _log = log;
    _fib = fib;
    _rib = rib;
    _ospf = ospf;
    _isis = isis;
    _bgp = bgp;
    _topo = topo;
  }

  public static class Builder {
    public Path _outputBase;
    public Path _log;
    public Path _fib;
    public Path _rib;
    public Path _ospf;
    public Path _isis;
    public Path _bgp;
    public Path _topo;

    public Builder setOutputBase(Path outputBase) {
      _outputBase = outputBase;
      return this;
    }

    public Builder setLog(Path log) {
      _log = log;
      return this;
    }

    public Builder setFib(Path fib) {
      _fib = fib;
      return this;
    }

    public Builder setRib(Path rib) {
      _rib = rib;
      return this;
    }

    public Builder setOspf(Path ospf) {
      _ospf = ospf;
      return this;
    }

    public Builder setIsis(Path isis) {
      _isis = isis;
      return this;
    }

    public Builder setBgp(Path bgp) {
      _bgp = bgp;
      return this;
    }

    public Builder setTopo(Path topo) {
      _topo = topo;
      return this;
    }

    public Output build() {
      String suffix =
          (Configuration.SYMBOLIC_COMMUNITY
                  ? "_comm" + (Configuration.SYMBOLIC_COMMUNITY_AP ? "_ap" : "")
                  : "")
              + (Configuration.SYMBOLIC_AS_PATH
                  ? "_asp" + (Configuration.SYMBOLIC_AS_PATH_AP ? "_ap" : "")
                  : "");
      return new Output(
          Objects.requireNonNull(_outputBase),
          _log == null ? _outputBase.resolve("log" + Controller.ID + ".txt") : _log,
          _fib == null ? _outputBase.resolve("fib" + suffix) : _fib,
          _rib == null ? _outputBase.resolve("rib" + suffix) : _rib,
          _ospf == null ? _outputBase.resolve("ospf" + suffix) : _ospf,
          _isis == null ? _outputBase.resolve("isis" + suffix) : _isis,
          _bgp == null ? _outputBase.resolve("bgp" + suffix) : _bgp,
          _topo == null ? _outputBase.resolve("topologies" + suffix) : _topo);
    }
  }
}
