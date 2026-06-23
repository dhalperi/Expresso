package datamodel;

import org.junit.Test;

import static org.junit.Assert.*;

public class RangeTest {
    @Test
    public void testConstructor() {
        Range<Integer> range = new Range<>(12, 11);
        System.out.println(range);
    }
}