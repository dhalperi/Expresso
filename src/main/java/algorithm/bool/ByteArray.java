package algorithm.bool;

import util.MathUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ByteArray extends Minterm {
  /**
   * low bit -> high bit e.g., decimal=4, nVar=4, binary=[0, 0, 1, 0] see {@link
   * MathUtil#calBinRep(long, int)}
   */
  byte[] binary;

  public ByteArray(int decimal, int nVar) {
    this.binary = MathUtil.calBinRep(decimal, nVar);
    this.nVar = nVar;
    this.nOne = countOne(binary);
    this.members = new HashSet<>();
    this.members.add(decimal);
  }

  public ByteArray(byte[] binary, int nOne, HashSet<Integer> members) {
    this.binary = binary;
    this.nVar = binary.length;
    this.nOne = nOne;
    this.members = members;
  }

  public static int countOne(byte[] bv) {
    int n = 0;
    for (byte bit : bv) {
      if (bit == 1) n++;
    }
    return n;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ByteArray that = (ByteArray) o;
    return Arrays.equals(binary, that.binary);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(binary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = binary.length - 1; i >= 0; i--) {
      byte bit = binary[i];
      if (bit == 0) {
        sb.append(0);
      } else if (bit == 1) {
        sb.append(1);
      } else {
        sb.append("X");
      }
    }
    return sb.toString();
  }

  @Override
  public String boolString() {
    StringBuilder sb = new StringBuilder();
    for (int i = binary.length - 1; i >= 0; i--) {
      byte bit = binary[i];
      char j = (char) ('A' + binary.length - 1 - i);
      if (bit == 0) {
        sb.append(j).append("'");
      } else if (bit == 1) {
        sb.append(j);
      }
    }
    return sb.toString();
  }

  @Override
  public String boolString(List<String> vars) {
    List<String> list = new LinkedList<>();
    for (int i = binary.length - 1; i >= 0; i--) {
      byte bit = binary[i];
      if (bit == 0) {
        list.add("!" + vars.get(i));
      } else if (bit == 1) {
        list.add(vars.get(i));
      }
    }
    return String.join(" & ", list);
  }

  @Override
  public ByteArray pair(Minterm another) {
    if (another instanceof ByteArray) {
      ByteArray that = (ByteArray) another;
      byte[] pairedBinary = Arrays.copyOf(this.binary, this.binary.length);
      int flag = -1;
      for (int i = 0; i < this.binary.length; i++) {
        if (this.binary[i] != that.binary[i]) {
          if (flag == -1) {
            pairedBinary[i] = -1;
            flag = i;
          } else {
            return null;
          }
        }
      }
      HashSet<Integer> pairedMembers = new HashSet<>(this.members);
      pairedMembers.addAll(that.members);
      return new ByteArray(
          pairedBinary, this.nOne - ((flag >= 0 && this.binary[flag] == 1) ? 1 : 0), pairedMembers);
    }
    return null;
  }

  @Override
  public byte[] getBinary() {
    return binary;
  }
}
