package datamodel.path;

import com.fasterxml.jackson.annotation.JsonValue;

public interface PathHop {
  @JsonValue
  String hopString();
}
