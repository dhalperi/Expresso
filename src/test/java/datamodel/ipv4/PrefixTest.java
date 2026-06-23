package datamodel.ipv4;

import org.junit.Test;

import static org.junit.Assert.*;

public class PrefixTest {

    @Test
    public void match() {
        Prefix prefix = Prefix.of(Ip.of("192.168.0.0"), Mask.of(16));
        Prefix p1 = Prefix.of(Ip.of("192.168.0.1"), Mask.of(24));
        Prefix p2 = Prefix.of(Ip.of("192.168.5.1"), Mask.of(8));
        assertTrue(prefix.ipMatch(Ip.of("192.168.1.0")));
        assertTrue(prefix.prefixMatchLoose(p1));
        assertFalse(prefix.prefixMatchLoose(p2));
    }
}
