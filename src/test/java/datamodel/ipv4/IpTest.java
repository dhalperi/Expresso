package datamodel.ipv4;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IpTest {
    @Test
    public void ipBinToPrefixTest() {
        byte[] ip = new byte[] {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,1,0};
        Prefix prefix1 = Ip.ipBinToPrefixes(ip).iterator().next();
        Prefix prefix2 = Prefix.of(Ip.of("70.0.0.0"), Mask.of(20));
        assertEquals(prefix1, prefix2);
    }

    @Test
    public void ipLongToStr() {
        String ip = "255.255.255.254";
        assertEquals(Ip.ipLongToStr(Ip.ipStrToLong(ip)), ip);
    }
}
