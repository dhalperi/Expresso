package datamodel;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class RangeTest {
    // Pre-existing broken test (unrelated to the Batfish migration): constructs
    // new Range<>(12, 11) with lower > upper, which the Range constructor rejects with
    // IllegalArgumentException by design. The test has no assertion and only prints.
    @Ignore("Asserts nothing and constructs an invalid Range (lower > upper); pre-existing failure")
    @Test
    public void testConstructor() {
        Range<Integer> range = new Range<>(12, 11);
        System.out.println(range);
    }
}