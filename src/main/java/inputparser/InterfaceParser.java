package inputparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import controlplane.network.Interface;
import controlplane.network.InterfaceName;
import controlplane.network.Router;
import controlplane.process.ospf.OspfInterfaceType;
import controlplane.process.ospf.OspfIntfSetting;
import datamodel.ipv4.IpAddress;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

import static util.JsonUtil.*;

public class InterfaceParser {
  public static Multimap<String, InterfaceName> parseInterfaces(
      Router router, JsonObject jsonInterfaces) {
    Multimap<String, InterfaceName> vrfInterfaces = HashMultimap.create();
    jsonInterfaces
        .entrySet()
        .forEach(
            entry -> parseInterface(entry.getValue().getAsJsonObject(), router, vrfInterfaces));
    return vrfInterfaces;
  }

  public static void parseInterface(
      JsonObject jsonInterface, Router router, Multimap<String, InterfaceName> vrfInterfaces) {
    Interface.Builder builder = Interface.builder();

    // set router
    builder.setRouter(router);

    // set interface name
    String name = jsonInterface.get("name").getAsString();
    InterfaceName interfaceName = new InterfaceName(router.getRouterName(), name);
    builder.setInterfaceName(interfaceName);

    // set interface type
    builder.setType(Interface.InterfaceType.valueOf(jsonInterface.get("type").getAsString()));

    // set primary ip address
    IpAddress primaryIpAddress = null;
    if (!jsonInterface.get("prefix").isJsonNull()) {
      primaryIpAddress = IpAddress.parse(jsonInterface.get("prefix").getAsString());
    }
    builder.setPrimaryIpAddress(primaryIpAddress);

    // set secondary ip addresses
    List<IpAddress> secondaryIpAddresses = new ArrayList<>();
    for (JsonElement e : jsonInterface.getAsJsonArray("allPrefixes")) {
      IpAddress ipAddress = IpAddress.parse(e.getAsString());
      if (!ipAddress.equals(primaryIpAddress)) {
        secondaryIpAddresses.add(ipAddress);
      }
    }
    builder.setSecondaryIpAddresses(secondaryIpAddresses);

    // set active
    builder.setActive(jsonInterface.get("active").getAsBoolean());

    // set channel group
    builder.setChannelGroup(getAsStringDefaultNull(jsonInterface, "channelGroup"));

    // set bandwidth
    builder.setBandwidth(getAsDoubleOrDefault(jsonInterface, "bandwidth", 1.0E10));

    // set ospf interface setting
    builder.setOspfIntfSetting(parseOspfIntfSetting(jsonInterface));

    // set isis interface setting
    builder.setIsisIntfSetting(parseIsisIntfSetting(jsonInterface));

    // set inbound and outbound ACLs
    if (!jsonInterface.get("incomingFilter").isJsonNull()) {
      builder.setInboundAcl(router.getAcl(jsonInterface.get("incomingFilter").getAsString()));
    }
    if (!jsonInterface.get("outgoingFilter").isJsonNull()) {
      builder.setOutboundAcl(router.getAcl(jsonInterface.get("outgoingFilter").getAsString()));
    }

    // set inbound and outbound traffic policies
    String inboundTrafficPolicy =
        JsonUtil.getAsStringDefaultNull(jsonInterface, "inboundPacketPolicy");
    if (inboundTrafficPolicy != null) {
      builder.setInboundTrafficPolicy(router.getTrafficPolicy(inboundTrafficPolicy));
    }
    String outboundTrafficPolicy =
        JsonUtil.getAsStringDefaultNull(jsonInterface, "outboundPacketPolicy");
    if (outboundTrafficPolicy != null) {
      builder.setOutboundTrafficPolicy(router.getTrafficPolicy(outboundTrafficPolicy));
    }

    // vrf
    vrfInterfaces.put(jsonInterface.get("vrf").getAsString(), interfaceName);

    router.getInterfaces().put(interfaceName, builder.build());
  }

  public static OspfIntfSetting parseOspfIntfSetting(JsonObject jsonInterface) {
    if (jsonInterface.get("ospfSettings") != null) {
      // new json configuration format
      OspfIntfSetting.Builder builder = new OspfIntfSetting.Builder();

      if (jsonInterface.get("ospfSettings").isJsonNull()) {
        builder.setOspfEnabled(false);
      } else {
        JsonObject jsonOspfSettings = jsonInterface.getAsJsonObject("ospfSettings");

        if (!jsonOspfSettings.get("area").isJsonNull()) {
          builder.setOspfArea(jsonOspfSettings.get("area").getAsLong());
        }

        if (!jsonOspfSettings.get("cost").isJsonNull()) {
          builder.setOspfCost(jsonOspfSettings.get("cost").getAsInt());
        }

        builder.setOspfEnabled(jsonOspfSettings.get("enabled").getAsBoolean());
        builder.setOspfPassive(jsonOspfSettings.get("passive").getAsBoolean());
        String networkType = getAsStringOrDefault(jsonOspfSettings, "networkType", "BROADCAST");
        networkType = networkType.equals("POINT_TO_POINT") ? "P2P" : networkType;
        builder.setOspfInterfaceType(OspfInterfaceType.valueOf(networkType));
      }

      return builder.build();
    } else {
      // old json configuration format
      OspfIntfSetting.Builder builder = new OspfIntfSetting.Builder();

      if (!jsonInterface.get("ospfArea").isJsonNull()) {
        builder.setOspfArea(jsonInterface.get("ospfArea").getAsLong());
      }

      if (!jsonInterface.get("ospfCost").isJsonNull()) {
        builder.setOspfCost(jsonInterface.get("ospfCost").getAsInt());
      }

      builder.setOspfEnabled(jsonInterface.get("ospfEnabled").getAsBoolean());
      builder.setOspfPassive(jsonInterface.get("ospfPassive").getAsBoolean());
      builder.setOspfInterfaceType(
          OspfInterfaceType.valueOf(jsonInterface.get("ospfInterfaceType").getAsString()));

      return builder.build();
    }
  }

  public static IsisInterfaceSettings parseIsisIntfSetting(JsonObject jsonInterface) {
    try {
      JsonElement jsonIsis = jsonInterface.get("isis");
      if (!jsonIsis.isJsonNull()) {
        return JsonUtil.mapper.readValue(
            jsonIsis.getAsJsonObject().toString(), IsisInterfaceSettings.class);
      }
    } catch (JsonProcessingException ignored) {
    }
    return null;
  }
}
