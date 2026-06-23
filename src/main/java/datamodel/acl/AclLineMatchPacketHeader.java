package datamodel.acl;

import com.google.common.base.MoreObjects;
import datamodel.Range;
import datamodel.ipv4.Prefix;

public class AclLineMatchPacketHeader extends AclLineMatch {
  private final Range<Long> protocol;
  private final Prefix sourceIp;
  private final Range<Long> sourcePort;
  private final Prefix destinationIp;
  private final Range<Long> destinationPort;

  public Range<Long> getProtocol() {
    return protocol;
  }

  public Prefix getSourceIp() {
    return sourceIp;
  }

  public Range<Long> getSourcePort() {
    return sourcePort;
  }

  public Prefix getDestinationIp() {
    return destinationIp;
  }

  public Range<Long> getDestinationPort() {
    return destinationPort;
  }

  @Override
  public <T> T accept(GenericAclLineMatchVisitor<T> visitor) {
    return visitor.visitAclLineMatchPacketHeader(this);
  }

  private AclLineMatchPacketHeader(
      Range<Long> protocol,
      Prefix sourceIp,
      Range<Long> sourcePort,
      Prefix destinationIp,
      Range<Long> destinationPort) {
    this.protocol = protocol;
    this.sourceIp = sourceIp;
    this.sourcePort = sourcePort;
    this.destinationIp = destinationIp;
    this.destinationPort = destinationPort;
  }

  public static class Builder {
    public Range<Long> protocol;
    public Prefix sourceIp;
    public Range<Long> sourcePort;
    public Prefix destinationIp;
    public Range<Long> destinationPort;

    public Builder setProtocol(Range<Long> protocol) {
      this.protocol = protocol;
      return this;
    }

    public Builder setSourceIp(Prefix sourceIp) {
      this.sourceIp = sourceIp;
      return this;
    }

    public Builder setSourcePort(Range<Long> sourcePort) {
      this.sourcePort = sourcePort;
      return this;
    }

    public Builder setDestinationIp(Prefix destinationIp) {
      this.destinationIp = destinationIp;
      return this;
    }

    public Builder setDestinationPort(Range<Long> destinationPort) {
      this.destinationPort = destinationPort;
      return this;
    }

    public AclLineMatchPacketHeader build() {
      return new AclLineMatchPacketHeader(
          protocol, sourceIp, sourcePort, destinationIp, destinationPort);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("protocol", protocol)
        .add("sourceIp", sourceIp)
        .add("sourcePort", sourcePort)
        .add("destinationIp", destinationIp)
        .add("destinationPort", destinationPort)
        .toString();
  }
}
