package algorithm.bool;

import com.google.common.base.MoreObjects;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Examples: [0, X, 0, 1], decimal = 1 (0001), wildcard = 4 (0100) [X, 0, 0, X], decimal = 0 (0000),
 * wildcard = 9 (1001)
 */
public class Decimal extends Minterm {
  int decimal;
  int wildcard;

  public Decimal(int decimal, int nVar) {
    this.decimal = decimal;
    this.wildcard = 0;
    this.nVar = nVar;
    this.nOne = countOne(decimal);
    this.members = new HashSet<>();
    this.members.add(decimal);
  }

  public Decimal(int decimal, int wildcard, int nOne, int nVar, HashSet<Integer> members) {
    this.decimal = decimal;
    this.wildcard = wildcard;
    this.nVar = nVar;
    this.nOne = nOne;
    this.members = members;
  }

  public static int countOne(int decimal) {
    int n = 0;
    int tmp = decimal;
    while (tmp != 0) {
      n += (tmp & 1);
      tmp >>= 1;
    }
    return n;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Decimal decimal1 = (Decimal) o;
    return decimal == decimal1.decimal && wildcard == decimal1.wildcard;
  }

  @Override
  public int hashCode() {
    return Objects.hash(decimal, wildcard);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    int tmp1 = decimal;
    int tmp2 = wildcard;
    while (i < nVar) {
      sb.append((tmp2 & 1) == 1 ? "X" : "" + (tmp1 & 1));
      i++;
      tmp1 >>= 1;
      tmp2 >>= 1;
    }
    return MoreObjects.toStringHelper(this)
        .add("bool", sb.reverse().toString())
        .add("nOne", nOne)
        .add("members", members)
        .toString();
    //        return sb.reverse().toString();
  }

  @Override
  public String boolString() {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    int tmp1 = decimal;
    int tmp2 = wildcard;
    while (i < nVar) {
      // is not wildcard bit
      if ((tmp2 & 1) == 0) {
        if ((tmp1 & 1) == 0) sb.append("'");
        sb.append((char) ('A' + nVar - i - 1));
      }
      i++;
      tmp1 >>= 1;
      tmp2 >>= 1;
    }
    return sb.reverse().toString();
  }

  @Override
  public String boolString(List<String> vars) {
    List<String> list = new LinkedList<>();
    int tmp1 = decimal;
    int tmp2 = wildcard;
    for (String var : vars) {
      // is not wildcard bit
      if ((tmp2 & 1) == 0) {
        if ((tmp1 & 1) == 0) list.add("!" + var);
        else list.add(var);
      }
      tmp1 >>= 1;
      tmp2 >>= 1;
    }
    return String.join(" & ", list);
  }

  @Override
  public Decimal pair(Minterm another) {
    if (another instanceof Decimal) {
      Decimal that = (Decimal) another;
      int tmp1 = this.decimal ^ that.decimal;
      int tmp2 = this.wildcard ^ that.wildcard;
      if (countOne(tmp1) + countOne(tmp2) <= 1) {
        int pairedDecimal = this.decimal & that.decimal;
        int pairedWildcard = this.wildcard | that.wildcard | tmp1;
        HashSet<Integer> pairedMembers = new HashSet<>(this.members);
        pairedMembers.addAll(that.members);
        return new Decimal(
            pairedDecimal, pairedWildcard, countOne(pairedDecimal), this.nVar, pairedMembers);
      }
    }
    return null;
  }

  @Override
  public byte[] getBinary() {
    byte[] binary = new byte[nVar];
    int i = 0;
    int tmp1 = decimal;
    int tmp2 = wildcard;
    while (i < nVar) {
      if ((tmp2 & 1) == 1) binary[i] = -1;
      else binary[i] = (byte) (tmp1 & 1);
      i++;
      tmp1 >>= 1;
      tmp2 >>= 1;
    }
    return binary;
  }
}
