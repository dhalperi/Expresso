package datamodel.path;

import datamodel.community.Community;
import datamodel.community.CommunityRegex;
import datamodel.community.StandardCommunity;

/** BTE is short for Block To External, used to check the BlockToExternal property in Bagpipe. */
public class BTE implements PathHop {
  public static final BTE INSTANCE = new BTE();
  private static final Community COMM = StandardCommunity.parse("11537:888");
  private static final CommunityRegex REGEX = CommunityRegex.from(COMM); // specific to internet2

  private BTE() {}

  public static CommunityRegex getCommunityRegex() {
    return REGEX;
  }

  public static Community getComm() {
    return COMM;
  }

  @Override
  public String hopString() {
    return String.format("BTE(%s)", COMM.toString());
  }
}
