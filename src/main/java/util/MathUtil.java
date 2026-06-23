package util;

import datamodel.Range;

import java.util.*;

public class MathUtil {

  /** only used to compute no more than 2^16 */
  public static long power2(int exponent) {
    if (exponent <= 16) {
      switch (exponent) {
        case 0:
          return 1;
        case 1:
          return 2;
        case 2:
          return 4;
        case 3:
          return 8;
        case 4:
          return 16;
        case 5:
          return 32;
        case 6:
          return 64;
        case 7:
          return 128;
        case 8:
          return 256;
        case 9:
          return 512;
        case 10:
          return 1024;
        case 11:
          return 2048;
        case 12:
          return 4096;
        case 13:
          return 8192;
        case 14:
          return 16384;
        case 15:
          return 32768;
        case 16:
          return 65536;
        default:
          System.err.println("exponent is too large!");
          break;
      }
    } else {
      long power = 1;
      for (int i = 0; i < exponent; i++) {
        power = power * 2;
      }
      return power;
    }
    // should not be here
    return 0;
  }

  /** ex1 < ex2 return 2^ex1 + 2^(ex1+1) + ... + 2^ex2 */
  public static long sumPower2(int ex1, int ex2) {
    long sum = 0;
    for (int i = ex1; i <= ex2; i++) {
      sum = sum + power2(i);
    }
    return sum;
  }

  /**
   * the input num is at most bits long find the number of tailing zeros in the binary
   * representation of num for example, num = 6 = (110)_2, bits = 3, return 1
   */
  public static int tailingZeros(long num, int bits) {
    if (num == 0) return bits;
    int tmpTailing = 0;
    for (int i = 0; i < bits; i++) {
      long tester = power2(i) - 1;
      long tn = num & tester;
      if (tn == 0) {
        tmpTailing = i;
      } else {
        break;
      }
    }
    return tmpTailing;
  }

  /**
   * return the binary representation of num <b>(low bits -> high bits)</b> e.g. num = 10, bits = 4,
   * return an array of {0,1,0,1}
   */
  public static byte[] calBinRep(long num, int bits) {
    if (bits == 0) return new byte[0];

    byte[] binRep = new byte[bits];
    long numTemp = num;
    for (int i = bits; i > 0; i--) {
      long aBit = numTemp & power2(i - 1);
      if (aBit == 0) {
        binRep[i - 1] = 0;
      } else {
        binRep[i - 1] = 1;
      }
      numTemp = numTemp - aBit;
    }
    return binRep;
  }

  /** usually bits = 8 or 16 */
  public static LinkedList<byte[]> decomposeInterval(Range<Integer> r, int bits) {
    long l = r.getLowerBound();
    long u = r.getUpperBound();
    LinkedList<byte[]> prefix = new LinkedList<>();

    while (l <= u) {
      l = onePrefix(l, u, bits, prefix);
    }

    return prefix;
  }

  /** creat one prefix from [l, u] */
  public static long onePrefix(long l, long u, int bits, LinkedList<byte[]> prefixes) {
    int zeros = tailingZeros(l, bits);
    if (zeros == 0) {
      prefixes.add(calBinRep(l, bits));
      return l + 1;
    } else {
      while (l + power2(zeros) > u + 1) {
        zeros--;
      }
      prefixes.add(calBinRep(l / power2(zeros), bits - zeros));
      return l + power2(zeros);
    }
  }

  public static Collection<Range<Integer>> bitVector2Range(byte[] bv) {
    return bitVector2Range(bv, Integer.MAX_VALUE);
  }

  public static Collection<Range<Integer>> bitVector2Range(byte[] bv, int upperBound) {
    int bound = -1;
    while (bound + 1 < bv.length && bv[bound + 1] == -1) bound++;

    Vector<Integer> lowerBounds = new Vector<>();
    lowerBounds.add(0);
    int i = bv.length - 1;
    while (i > bound) {
      if (bv[i] != -1) {
        for (int j = 0; j < lowerBounds.size(); j++) {
          lowerBounds.set(j, (lowerBounds.get(j) << 1) + bv[i]);
        }
      } else {
        // we meet a wildcard bit
        Vector<Integer> tmp = new Vector<>(lowerBounds.size() * 2);
        for (int j = 0; j < lowerBounds.size(); j++) {
          int wildcard = lowerBounds.get(j) << 1;
          tmp.add(2 * j, wildcard);
          tmp.add(2 * j + 1, wildcard + 1);
        }
        lowerBounds = tmp;
      }
      i--;
    }
    for (int j = 0; j < lowerBounds.size(); j++) {
      lowerBounds.set(j, lowerBounds.get(j) << (bound + 1));
    }
    int rangeLen = (1 << (bound + 1)) - 1;

    HashSet<Range<Integer>> ranges = new HashSet<>();
    for (int lowerBound : lowerBounds) {
      if (lowerBound > upperBound) {
        break;
      }
      ranges.add(new Range<>(lowerBound, Integer.min(lowerBound + rangeLen, upperBound)));
    }
    return ranges;
  }

  /**
   * Combine integer ranges if possible. For example: <br>
   * (1) [1,7] and [2,4] -> [1,7] <br>
   * (2) [1,3] and [2,4] -> [1,4] <br>
   * (3) [1,3] and [5,8] -> [1,3], [5,8] <br>
   * (4) [1,3] and [4,5] -> [1,5] <br>
   */
  public static ArrayList<Range<Integer>> combineRanges(Collection<Range<Integer>> ranges) {
    ArrayList<Range<Integer>> ret = new ArrayList<>();
    ArrayList<Range<Integer>> list = new ArrayList<>(ranges);
    list.sort(null);
    int j = 0;
    while (j < list.size()) {
      int k = j + 1;
      int lowerBound = list.get(j).getLowerBound();
      int upperBound = list.get(j).getUpperBound();
      while (k < list.size() && list.get(k).getLowerBound() <= upperBound + 1) {
        upperBound = Integer.max(list.get(k).getUpperBound(), upperBound);
        k++;
      }
      ret.add(new Range<>(lowerBound, upperBound));
      j = k;
    }
    return ret;
  }
}
