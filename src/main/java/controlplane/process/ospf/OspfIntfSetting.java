package controlplane.process.ospf;

public class OspfIntfSetting {
  final long ospfArea;
  int ospfCost;
  final boolean ospfEnabled;
  final boolean ospfPassive;
  final OspfInterfaceType ospfInterfaceType;

  public OspfIntfSetting(
      long ospfArea,
      int ospfCost,
      boolean ospfEnabled,
      boolean ospfPassive,
      OspfInterfaceType ospfInterfaceType) {
    this.ospfArea = ospfArea;
    this.ospfCost = ospfCost;
    this.ospfEnabled = ospfEnabled;
    this.ospfPassive = ospfPassive;
    this.ospfInterfaceType = ospfInterfaceType;
  }

  public long getOspfArea() {
    return ospfArea;
  }

  public void setOspfCost(int ospfCost) {
    this.ospfCost = ospfCost;
  }

  public int getOspfCost() {
    return ospfCost;
  }

  public static class Builder {
    long ospfArea;
    int ospfCost = -1;
    boolean ospfEnabled;
    boolean ospfPassive;
    OspfInterfaceType ospfInterfaceType;

    public OspfIntfSetting build() {
      return new OspfIntfSetting(ospfArea, ospfCost, ospfEnabled, ospfPassive, ospfInterfaceType);
    }

    public Builder setOspfArea(long ospfArea) {
      this.ospfArea = ospfArea;
      return this;
    }

    public Builder setOspfCost(int ospfCost) {
      this.ospfCost = ospfCost;
      return this;
    }

    public Builder setOspfEnabled(boolean ospfEnabled) {
      this.ospfEnabled = ospfEnabled;
      return this;
    }

    public Builder setOspfPassive(boolean ospfPassive) {
      this.ospfPassive = ospfPassive;
      return this;
    }

    public Builder setOspfInterfaceType(OspfInterfaceType ospfInterfaceType) {
      this.ospfInterfaceType = ospfInterfaceType;
      return this;
    }
  }
}
