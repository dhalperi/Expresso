package main.storage;

import java.nio.file.Path;
import java.util.Objects;

public class Input {
  public Path _inputBase;
  public Path _config;
  public Path _acl;
  public Path _l3Edges;
  public Path _edgePorts;
  public Path _property;
  public Path _environment;

  public Input(
      Path inputBase,
      Path config,
      Path acl,
      Path l3Edges,
      Path edgePorts,
      Path property,
      Path environment) {
    _inputBase = inputBase;
    _config = config;
    _acl = acl;
    _l3Edges = l3Edges;
    _edgePorts = edgePorts;
    _property = property;
    _environment = environment;
  }

  public static class Builder {
    public Path _inputBase;
    public Path _config;
    public Path _acl;
    public Path _l3Edges;
    public Path _edgePorts;
    public Path _property;
    public Path _environment;

    public Builder setInputBase(Path inputBase) {
      _inputBase = inputBase;
      return this;
    }

    public Builder setConfig(Path config) {
      _config = config;
      return this;
    }

    public Builder setAcl(Path acl) {
      _acl = acl;
      return this;
    }

    public Builder setL3Edge(Path l3Edge) {
      _l3Edges = l3Edge;
      return this;
    }

    public Builder setEdgePorts(Path edgePorts) {
      _edgePorts = edgePorts;
      return this;
    }

    public Builder setProperty(Path property) {
      _property = property;
      return this;
    }

    public Builder setEnvironment(Path environment) {
      _environment = environment;
      return this;
    }

    public Input build() {
      return new Input(
          Objects.requireNonNull(_inputBase),
          _config == null ? _inputBase.resolve("configs") : _config,
          _acl == null ? _inputBase.resolve("acls") : _acl,
          _l3Edges == null ? _inputBase.resolve("topology.txt") : _l3Edges,
          _edgePorts == null ? _inputBase.resolve("edgePorts") : _edgePorts,
          _property == null ? _inputBase.resolve("probability.json") : _property,
          _environment == null ? _inputBase.resolve("environment.json") : _environment);
    }
  }
}
