package main;

import datamodel.vendor.Cisco;
import datamodel.vendor.Vendor;

public class Configuration {
  public static final int ENVC_PERM_SIZE = 10;
  public static final boolean ISP_VAR = true;

  /* Default vendor for devices */
  public static Vendor DEFAULT_VENDOR = Cisco.CISCO;

  /* BDDManager configurations */
  // BDD node table size.
  public static int DEFAULT_BDD_NT_SIZE = (int) 1e8;
  public static int DEFAULT_BDD_CACHE_SIZE = DEFAULT_BDD_NT_SIZE / 10;
  // If equals -1, optimize it according to network configurations.
  public static int BDD_NT_SIZE = -1;

  // Maximum route computation rounds for BGP and OSPF
  public static int MAX_SRC_ROUNDS = 100;
  // Route computation timeout in seconds
  public static int SRC_TIMEOUT_SECONDS = 3600;

  public static boolean BGP_SYNCHRONOUS = false;

  public static boolean LOG_ROUTE_DETAILS = false;

  /* SPF (symbolic packet forwarding) configurations */
  public static boolean ENABLE_ACL = true;
  public static boolean ENABLE_TRAFFIC_POLICY = true;

  /* Naming configurations */
  public static String DEFAULT_VRF_NAME = "default";
  public static String HOST_VRF_CONNECTOR = "-";
  public static String ROUTER_INTF_CONNECTOR = "#";

  public static String ASN_SEPARATOR = "_";

  public static boolean SYMBOLIC_COMMUNITY = false;
  public static boolean SYMBOLIC_COMMUNITY_AP = true;
  public static boolean SYMBOLIC_AS_PATH = false;
  public static boolean SYMBOLIC_AS_PATH_AP = false;
  public static boolean ABSTRACT_SET_AS_PATH = false;
}
