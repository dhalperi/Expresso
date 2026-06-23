package algorithm.bool;

import junit.framework.TestCase;

import java.util.HashSet;

public class DecimalTest extends TestCase {

    public void test1() {
        // [0, X, 0, 1], decimal = 1 (0001), wildcard = 4 (0100)
        int decimal = 1;
        int wildcard = 4;
        int nOne = 1;
        int nVar = 4;
        HashSet<Integer> members = new HashSet<>();
        members.add(1);
        members.add(5);
        Decimal d = new Decimal(decimal, wildcard, nOne, nVar, members);
        System.out.println(d);
    }
}