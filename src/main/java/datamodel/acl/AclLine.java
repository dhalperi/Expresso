package datamodel.acl;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

public class AclLine implements Serializable {

  private static final long serialVersionUID = 4261146103254314479L;

  public static final long minPort = 0L;
  public static final long maxPort = 65535L;
  public static final long minProtocol = 0;
  public static final long maxProtocol = 255;

  public String aclName;
  public int lineId;

  AclLineMatch match;
  AclLineAction action;

  public AclLineMatch getMatch() {
    return match;
  }

  public AclLineAction getAction() {
    return action;
  }

  public AclLine(String aclName, int lineId, AclLineMatch match, AclLineAction action) {
    this.aclName = aclName;
    this.lineId = lineId;
    this.match = match;
    this.action = action;
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("aclName", aclName)
        .add("lineId", lineId)
        .add("match", match)
        .add("action", action)
        .toString();
  }
}
