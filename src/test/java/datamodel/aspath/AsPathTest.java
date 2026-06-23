package datamodel.aspath;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AsPathTest {

    @Test
    public void ofSame() {
        AsPath asPath1 = AsPath.ofSingletonAsSets(55990L);
        List<AsSet> asSets = new ArrayList<>(asPath1.getAsSets());
        AsPath asPath2 = AsPath.of(asSets);
        asPath2.getAsSets().add(AsSet.of(55991));
        assertEquals(asPath1.getAsSets(), asPath2.getAsSets());
    }

    @Test
    public void ofDifferent() {
        AsPath asPath1 = AsPath.ofSingletonAsSets(55990L);
        List<AsSet> asSets = new ArrayList<>(asPath1.getAsSets());
        asSets.add(AsSet.of(55991L));
        AsPath asPath2 = AsPath.of(asSets);
        assertNotEquals(asPath1.getAsSets(), asPath2.getAsSets());
    }

    @Test
    public void equals() {
        AsPath asPath1 = AsPath.ofSingletonAsSets(55990L);
        AsPath asPath2 = AsPath.ofSingletonAsSets(55990L);
        assertEquals(asPath1, asPath2);
        assertEquals(asPath1.hashCode(), asPath2.hashCode());
    }
}