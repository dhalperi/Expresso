package algorithm.bool;

import junit.framework.TestCase;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QuineMcCluskeyTest extends TestCase {

    public void test1() {
        int nVar = 4;
        int[] trueMinterms = new int[] {4 ,5, 6, 9, 11, 12, 13, 14};
        int[] dontCareMinterms = new int[] {0, 1, 3, 7};
        QuineMcCluskey alg = new ByteArrayQMC(
                nVar,
                IntStream.of(trueMinterms).boxed().collect(Collectors.toSet()),
                IntStream.of(dontCareMinterms).boxed().collect(Collectors.toSet())
        );
        Set<Minterm> result = alg.minimize();
        System.out.println(result.stream().map(Minterm::boolString).collect(Collectors.joining("\n")));
    }

    public void test2() {
        int nVar = 2;
        byte[][] trueMinterms = new byte[][] {
                new byte[] {-1, 1},
                new byte[] {1, 0}
        };
        byte[][] dontCareMinterms = new byte[0][];
        QuineMcCluskey alg = new ByteArrayQMC(nVar, trueMinterms, dontCareMinterms);
        Set<Minterm> result = alg.minimize();
        System.out.println(result.stream().map(Minterm::boolString).collect(Collectors.joining("\n")));
    }

    public void test3() {
        int nVar = 4;
        int[] trueMinterms = new int[] {4 ,5, 6, 9, 11, 12, 13, 14};
        int[] dontCareMinterms = new int[] {0, 1, 3, 7};
        QuineMcCluskey alg = new DecimalQMC(
                nVar,
                IntStream.of(trueMinterms).boxed().collect(Collectors.toSet()),
                IntStream.of(dontCareMinterms).boxed().collect(Collectors.toSet())
        );
        Set<Minterm> result = alg.minimize();
        System.out.println(result.stream().map(Minterm::boolString).collect(Collectors.joining("\n")));
    }

    public void test4() {
        int nVar = 2;
        byte[][] trueMinterms = new byte[][] {
                new byte[] {-1, 1},
                new byte[] {1, 0}
        };
        byte[][] dontCareMinterms = new byte[0][];
        QuineMcCluskey alg = new DecimalQMC(nVar, trueMinterms, dontCareMinterms);
        Set<Minterm> result = alg.minimize();
        System.out.println(result.stream().map(Minterm::boolString).collect(Collectors.joining("\n")));
    }
}