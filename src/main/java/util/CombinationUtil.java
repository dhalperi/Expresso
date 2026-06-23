package util;

import java.util.HashSet;

public class CombinationUtil {
  public static long numItems(int n, int mf) {
    long num = 0;
    for (int i = 0; i <= mf; i++) num += choose(n, i);
    return num;
  }

  public static long choose(int n, int k) {
    int reflect = n - k;
    if (k > reflect) {
      if (k > n) return 0;
      k = reflect;
    }
    if (k == 0) return 1;
    else {
      long ans = n;
      for (int up = n - 1; up > n - k; up--) {
        int down = n + 1 - up;
        ans = ans * up / down;
      }
      return ans;
    }
  }

  @Deprecated /* the result may not be right */
  public static int[] nthCombination(int idx, int n, int k) {
    HashSet<Integer> hs = new HashSet<>();

    int cnk = 1;
    for (int i = n; i > n - k; i--) {
      int j = n + 1 - i;
      cnk = cnk * i / j;
    }
    int curIdx = cnk;

    while (k > 0) {
      cnk = cnk * k / n;
      while (curIdx - cnk > idx) {
        curIdx -= cnk;
        cnk *= (n - k);
        cnk -= cnk % k;
        n--;
        cnk /= n;
      }
      k--;
      n--;
      hs.add(n);
    }

    int[] combination = new int[n];
    for (int i = 0; i < n; i++) {
      if (hs.contains(i)) combination[i] = 0;
      else combination[i] = 1;
    }
    return combination;
  }

  @Deprecated /* the result may not be right */
  public static int indexOfCombination(int[] combination) {
    int idx = 0, k = 0;
    for (int i = 0; i < combination.length; i++) {
      if (combination[i] == 0) {
        k++;
        idx += choose(i, k);
      }
    }
    return idx;
  }

  @Deprecated /* the result may not be right */
  public static HashSet<Integer> rangeOfCombination(int[] combination, int kmax) {
    int cnt = 0;
    for (int i : combination) {
      if (i == 0) cnt++;
    }

    int low = indexOfCombination(combination);

    cnt = kmax - cnt;
    int[] tmp = combination.clone();
    for (int i = combination.length - 1; i >= 0 && cnt > 0; i--) {
      if (tmp[i] == -1) {
        tmp[i] = 0;
        cnt--;
      }
    }

    int high = indexOfCombination(tmp);

    HashSet<Integer> hs = new HashSet<>();
    for (int i = low; i <= high; i++) hs.add(i);
    return hs;
  }
}
