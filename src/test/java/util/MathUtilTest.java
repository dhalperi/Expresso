package util;

import com.google.common.collect.ImmutableList;
import datamodel.Range;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class MathUtilTest {

    @Test
    public void bitVector2Range() {
        byte[] rangeBin = new byte[] {0, 1};
        Collection<Range<Integer>> result = MathUtil.bitVector2Range(rangeBin);
        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), new Range<>(2));

        rangeBin = new byte[] {-1, 1};
        result = MathUtil.bitVector2Range(rangeBin);
        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), new Range<>(2, 3));

        rangeBin = new byte[] {-1, 1, -1, 0};
        result = MathUtil.bitVector2Range(rangeBin).stream().sorted().collect(Collectors.toList());
        assertEquals(result.size(), 2);
        Iterator<Range<Integer>> itr = result.iterator();
        assertEquals(itr.next(), new Range<>(2, 3));
        assertEquals(itr.next(), new Range<>(6, 7));

        rangeBin = new byte[] {-1, 1, -1, 0};
        result = MathUtil.bitVector2Range(rangeBin, 6).stream().sorted().collect(Collectors.toList());
        assertEquals(result.size(), 2);
        itr = result.iterator();
        assertEquals(itr.next(), new Range<>(2, 3));
        assertEquals(itr.next(), new Range<>(6, 6));

        rangeBin = new byte[] {1, 1, -1, 0};
        result = MathUtil.bitVector2Range(rangeBin, 6).stream().sorted().collect(Collectors.toList());
        assertEquals(result.size(), 1);
        itr = result.iterator();
        assertEquals(itr.next(), new Range<>(3, 3));
    }

    @Test
    public void combineRanges() {
        /* [1,7] and [2,4] -> [1,7] */
        Collection<Range<Integer>> ranges = ImmutableList.of(
                new Range<>(1, 7),
                new Range<>(2,4)
        );

        Collection<Range<Integer>> result = MathUtil.combineRanges(ranges);
        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), new Range<>(1, 7));

        /* [1,3] and [2,4] -> [1,4] */
        ranges = ImmutableList.of(
                new Range<>(1, 3),
                new Range<>(2, 4)
        );
        result = MathUtil.combineRanges(ranges);
        assertEquals(result.size(), 1);
        assertEquals(result.iterator().next(), new Range<>(1, 4));

        /* [1,3] and [5,8] -> [1,3], [5,8] */
        ranges = ImmutableList.of(
                new Range<>(1, 3),
                new Range<>(5, 8)
        );
        result = MathUtil.combineRanges(ranges);
        assertEquals(result, ranges);
    }
}