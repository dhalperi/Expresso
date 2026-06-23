package datamodel.ipv4;

import org.junit.Test;

import static org.junit.Assert.*;

public class MaskTest {

    @Test
    public void maskIpToLen() {
        String[] tmp = {
                "0.0.0.0",
                "128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
                "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
                "255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
                "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"};
        Mask[] masks = new Mask[33];
        for (int i = 0; i <= 32; i++) {
            masks[i] = Mask.of(Ip.of(tmp[i]));
            assertEquals(i, masks[i]._len);
        }
    }

    @Test
    public void maskLenToIp() {
//        Mask mask1 = Mask.of(32);
//        Mask mask2 = Mask.of(24);
//        Mask mask3 = Mask.of(16);
//        Mask mask4 = Mask.of(8);
//        Mask mask5 = Mask.of(31);
//        Mask mask6 = Mask.of(20);
//        Mask mask7 = Mask.of(0);
//        assertEquals("255.255.255.255", mask1._ip._ipStr);
//        assertEquals("255.255.255.0", mask2._ip._ipStr);
//        assertEquals("255.255.0.0", mask3._ip._ipStr);
//        assertEquals("255.0.0.0", mask4._ip._ipStr);
//        assertEquals("255.255.255.254", mask5._ip._ipStr);
//        assertEquals("255.255.240.0", mask6._ip._ipStr);
//        assertEquals("0.0.0.0", mask7._ip._ipStr);
        for (int i = 0; i <= 32; i++) {
            System.out.println(Mask.of(i)._ip.toString());
        }
    }
}
