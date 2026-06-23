package datamodel.aspath;

import main.Configuration;

import java.util.List;

public class AsPathFactory {
  public static AsPathIntf ofSingletonAsSets(Long... asNums) {
    if (Configuration.SYMBOLIC_AS_PATH) {
      return SymbolicAsPath.ofSingletonAsSets(asNums);
    } else {
      return AsPath.ofSingletonAsSets(asNums);
    }
  }

  public static AsPathIntf ofSingletonAsSets(List<Long> asNums) {
    if (Configuration.SYMBOLIC_AS_PATH) {
      return SymbolicAsPath.ofSingletonAsSets(asNums);
    } else {
      return AsPath.ofSingletonAsSets(asNums);
    }
  }

  public static AsPathIntf empty() {
    if (Configuration.SYMBOLIC_AS_PATH) {
      return SymbolicAsPath.empty();
    } else {
      return AsPath.empty();
    }
  }
}
