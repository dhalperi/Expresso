package atomic;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jdd.bdd.BDD;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class BDDTCWrapperTest {

    BDD bdd;
    int num;

//    public HashSet<byte[]> allSat(int tc) {
//        HashSet<byte[]> ret = new HashSet<>();
//        byte[] state = new byte[num];
//        Arrays.fill(state, (byte) - 1);
//        allSat_rec(ret, state, tc);
//        return ret;
//    }
//
//    private void allSat_rec(HashSet<byte[]> ret, byte[] state, int tc) {
//        if (tc == 0) {
//            return;
//        }
//        if (tc == 1) {
//            byte[] tmp = state.clone();
//            ret.add(tmp);
//        }
//        else {
//            int var = bdd.getVar(tc);
//            state[var] = 0;
//            allSat_rec(ret, state, bdd.getLow(tc));
//            state[var] = 1;
//            allSat_rec(ret, state, bdd.getHigh(tc));
//            state[var] = -1;
//        }
//    }

    Multimap<Integer, byte[]> tcAllSatMemo;

    public Collection<byte[]> allSat(int tc) {
        if (tc == 0) {
            return new HashSet<>();
        }
        if (tcAllSatMemo == null) {
            tcAllSatMemo = HashMultimap.create();
            byte[] allSymbolic = new byte[num];
            Arrays.fill(allSymbolic, (byte) -1);
            tcAllSatMemo.put(1, allSymbolic);
        }
        if (!tcAllSatMemo.containsKey(tc)) {
            int var = bdd.getVar(tc);
            for (byte[] oneSat : allSat(bdd.getLow(tc))) {
                byte[] down = oneSat.clone();
                down[var] = 0;
                tcAllSatMemo.put(tc, down);
            }
            for (byte[] oneSat : allSat(bdd.getHigh(tc))) {
                byte[] up = oneSat.clone();
                up[var] = 1;
                tcAllSatMemo.put(tc, up);
            }
        }
        return tcAllSatMemo.get(tc);
    }

    @Test
    public void allSat() {
        bdd = new BDD(100);
        num = 3;
        int v1 = bdd.createVar();
        int v2 = bdd.createVar();
        int v3 = bdd.createVar();
        int v4 = bdd.ref(bdd.or(bdd.and(v1, v2), bdd.and(v2, v3)));
        for (byte[] state : allSat(v4)) {
            String[] str = new String[state.length];
            for (int i = 0; i < state.length; i++) {
                str[i] = String.valueOf(state[i]);
            }
            System.out.println(String.join(",", str));
        }
    }

    @Test
    public void test() {
        Person[] a = new Person[] {new Person("Simon", "Smith"), new Person("Daniel", "Watson")};
        Person[] b = a.clone();
        b[0].firstName = "John";
        System.out.println(Arrays.toString(a));
        System.out.println(Arrays.toString(b));
    }

    static class Person{
        String firstName;
        String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return Objects.equals(firstName, person.firstName) &&
                    Objects.equals(lastName, person.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName);
        }

        @Override
        public String toString() {
            return firstName + " " + lastName;
        }
    }
}
