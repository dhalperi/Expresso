package algorithm.bool;

import java.util.HashSet;
import java.util.List;

public abstract class Minterm {
  int nVar;
  int nOne;
  HashSet<Integer> members;

  public int getNumberOfOne() {
    return nOne;
  }

  public HashSet<Integer> getMembers() {
    return members;
  }

  public abstract String boolString();

  public abstract String boolString(List<String> vars);

  public abstract Minterm pair(Minterm another);

  public abstract byte[] getBinary();
}
