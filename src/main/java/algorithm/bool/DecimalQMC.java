package algorithm.bool;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DecimalQMC extends QuineMcCluskey {
  public DecimalQMC(int nVar, Set<Integer> trueMinterms, Set<Integer> dontCareMinterms) {
    super(nVar, trueMinterms, dontCareMinterms);
  }

  public DecimalQMC(int nVar, byte[][] trueMinterms, byte[][] dontCareMinterms) {
    super(nVar, trueMinterms, dontCareMinterms);
  }

  @Override
  Minterm getMinterm(int decimal) {
    return new Decimal(decimal, nVar);
  }

  @Override
  Minterm getMinterm(byte[] binary, int decimal, int nOne) {
    return new Decimal(
        decimal, 0, nOne, binary.length, new HashSet<>(Collections.singleton(decimal)));
  }
}
