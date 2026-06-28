package datamodel.community;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link ExtendedCommunity} parsing, in particular generic extended communities whose
 * type octet has the high bit set (e.g. Junos "65000:672277L:36867", type octet 0xFD). These must
 * parse and round-trip rather than being rejected as an out-of-range / invalid type, matching
 * Batfish's behavior.
 */
public class ExtendedCommunityTest {

  @Test
  public void parsesGenericHighTypeOctet() {
    // subType field "65000" -> type octet 0xFD, subtype octet 0xE8; 4-byte GA (L suffix); LA 36867.
    ExtendedCommunity ec = ExtendedCommunity.parse("65000:672277L:36867");
    assertEquals(672277L, ec.getGlobalAdministrator());
    assertEquals(36867L, ec.getLocalAdministrator());
    // An unrecognized type is not a route-target / route-origin.
    org.junit.Assert.assertFalse(ec.isRouteTarget());
    org.junit.Assert.assertFalse(ec.isRouteOrigin());
  }

  @Test
  public void roundTripsGenericHighTypeOctet() {
    String s = "65000:672277L:36867";
    assertEquals(s, ExtendedCommunity.parse(s).toString());
  }

  @Test
  public void parsesStandardTwoByteExtendedCommunity() {
    // type 0x02, GA 11537, LA 1
    ExtendedCommunity ec = ExtendedCommunity.parse("2:11537:1");
    assertEquals(11537L, ec.getGlobalAdministrator());
    assertEquals(1L, ec.getLocalAdministrator());
    assertEquals("2:11537:1", ec.toString());
  }

  @Test
  public void routeTargetIsRecognized() {
    ExtendedCommunity rt = ExtendedCommunity.target(65000L, 100L);
    org.junit.Assert.assertTrue(rt.isRouteTarget());
  }
}
