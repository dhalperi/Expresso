package inputparser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import datamodel.community.*;

public class CommunityParser {
    public static Community parseLiteralCommunity(JsonElement element) {
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        String literalCommunity = primitive.isString() ? primitive.getAsString() : String.valueOf(primitive.getAsLong());
        return parseCommunity(literalCommunity);
    }

    public static Community parseCommunity(String value) {
        if (value == null) {
            return null;
        }
        else {
            Community community = parseWellKnownCommunity(value);
            if (community != null) {
                return community;
            }
            else {
                int len = value.split(":").length;
                if (len <= 2) {
                    return StandardCommunity.parse(value);
                }
                else if (len == 3) {
                    return ExtendedCommunity.parse(value);
                }
                else {
                    return LargeCommunity.parse(value);
                }
            }
        }
    }

    public static Community parseWellKnownCommunity(String value) {
        switch (value) {
            case "internet":
                return StandardCommunity.INTERNET;
            case "graceful-shutdown":
                return StandardCommunity.GRACEFUL_SHUTDOWN;
            case "accept-own":
                return StandardCommunity.ACCEPT_OWN;
            case "router-filter-translated-v4":
                return StandardCommunity.ROUTE_FILTER_TRANSLATED_V4;
            case "router-filter-v4":
                return StandardCommunity.ROUTE_FILTER_V4;
            case "router-filter-translated-v6":
                return StandardCommunity.ROUTE_FILTER_TRANSLATED_V6;
            case "router-filter-v6":
                return StandardCommunity.ROUTE_FILTER_V6;
            case "llgr-stale":
                return StandardCommunity.LLGR_STALE;
            case "no-llgr":
                return StandardCommunity.NO_LLGR;
            case "accept-own-nexthop":
                return StandardCommunity.ACCEPT_OWN_NEXTHOP;
            case "blackhole":
                return StandardCommunity.BLACKHOLE;
            case "no-export":
                return StandardCommunity.NO_EXPORT;
            case "no-advertise":
                return StandardCommunity.NO_ADVERTISE;
            case "no-export-subconfed":
                return StandardCommunity.NO_EXPORT_SUBCONFED;
            case "no-peer":
                return StandardCommunity.NO_PEER;
            default:
                return null;
        }
    }
}
