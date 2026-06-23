package algorithm.bool;

import java.util.*;

public class ByteArrayQMC extends QuineMcCluskey {
  public ByteArrayQMC(int nVar, Set<Integer> trueMinterms, Set<Integer> dontCareMinterms) {
    super(nVar, trueMinterms, dontCareMinterms);
  }

  public ByteArrayQMC(int nVar, byte[][] trueMinterms, byte[][] dontCareMinterms) {
    super(nVar, trueMinterms, dontCareMinterms);
  }

  @Override
  Minterm getMinterm(int decimal) {
    return new ByteArray(decimal, nVar);
  }

  @Override
  Minterm getMinterm(byte[] binary, int decimal, int nOne) {
    return new ByteArray(binary, nOne, new HashSet<>(Collections.singleton(decimal)));
  }
}
