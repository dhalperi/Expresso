package inputparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import datamodel.Range;
import datamodel.ipv4.Prefix;
import datamodel.acl.*;

import java.util.*;
import java.util.stream.Collectors;

public class AclParser {
  public static SortedMap<String, Acl> parseAcls(JsonObject jsonAcls) {
    SortedMap<String, Acl> acls =
        jsonAcls.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> parseAcl(e.getValue().getAsJsonObject()),
                    (u, v) -> u,
                    TreeMap::new));
    acls.values()
        .forEach(
            acl ->
                acl.getLines()
                    .forEach(
                        line -> {
                          if (line.getMatch() instanceof AclLineMatchAcl) {
                            ((AclLineMatchAcl) line.getMatch())
                                .setReferenceAcl(
                                    acls.get(
                                        ((AclLineMatchAcl) line.getMatch()).getReferenceName()));
                          }
                        }));
    return acls;
  }

  public static Acl parseAcl(JsonObject jsonAcl) {
    String name = jsonAcl.get("name").getAsString();
    List<AclLine> lines = new LinkedList<>();
    JsonArray jsonAclLines = jsonAcl.getAsJsonArray("lines");
    for (int i = 0; i < jsonAclLines.size(); i++) {
      JsonObject jsonAclLine = jsonAclLines.get(i).getAsJsonObject();
      AclLine line = parseAclLine(name, i, jsonAclLine);
      lines.add(line);
    }
    return new Acl(name, lines);
  }

  public static AclLine parseAclLine(String aclName, int i, JsonObject jsonAclLine) {
    JsonElement tmp = jsonAclLine.get("name");
    boolean flag = !tmp.isJsonNull() && tmp.getAsString().chars().allMatch(Character::isDigit);
    int lineId = flag ? Integer.parseInt(tmp.getAsString()) : i;
    AclLineMatch match = parseAclLineMatch(jsonAclLine.getAsJsonObject("matchCondition"));
    AclLineAction action =
        jsonAclLine.get("action").getAsString().equalsIgnoreCase("permit")
            ? AclLineAction.PERMIT
            : AclLineAction.DENY;
    return new AclLine(aclName, lineId, match, action);
  }

  public static AclLineMatch parseAclLineMatch(JsonObject jsonAclLineMatch) {
    String clz = jsonAclLineMatch.get("class").getAsString();
    if (clz.contains("OrMatchExpr")) {
      // batfish OrMatchExpr
      JsonArray jsonDisjuncts = jsonAclLineMatch.getAsJsonArray("disjuncts");
      List<AclLineMatch> disjuncts = new LinkedList<>();
      for (int i = 0; i < jsonDisjuncts.size(); i++) {
        AclLineMatch disjunct = parseAclLineMatch(jsonDisjuncts.get(i).getAsJsonObject());
        disjuncts.add(disjunct);
      }
      return new AclLineMatchOr(disjuncts);
    } else if (clz.contains("PermittedByAcl")) {
      // batfish PermittedByAcl
      String aclReference = jsonAclLineMatch.get("aclName").getAsString();
      return new AclLineMatchAcl(aclReference);
    } else if (clz.contains("MatchHeaderSpace")) {
      // batfish MatchHeaderSpace
      JsonObject jsonHeaderSpace = jsonAclLineMatch.getAsJsonObject("headerSpace");
      Range<Long> protocol = new Range<>(AclLine.minProtocol, AclLine.maxPort); // todo
      Prefix srcIp =
          Prefix.of(
              jsonHeaderSpace
                  .get("srcIps")
                  .getAsJsonObject()
                  .get("ipWildcard")
                  .getAsString()); // batfish IpWildcard
      Range<Long> srcPort = new Range<>(AclLine.minPort, AclLine.maxPort); // todo
      Prefix dstIp =
          Prefix.of(
              jsonHeaderSpace
                  .get("dstIps")
                  .getAsJsonObject()
                  .get("ipWildcard")
                  .getAsString()); // batfish IpWildcard
      Range<Long> dstPort = new Range<>(AclLine.minPort, AclLine.maxPort); // todo
      return new AclLineMatchPacketHeader.Builder()
          .setProtocol(protocol)
          .setSourceIp(srcIp)
          .setSourcePort(srcPort)
          .setDestinationIp(dstIp)
          .setDestinationPort(dstPort)
          .build();
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
