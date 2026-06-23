package controlplane.process.bgp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import datamodel.ipv4.Ip;

import java.util.*;
import java.util.stream.Collectors;

public class BgpExternalPeer {
    static ISP generateIsp(Ip ip, long as, Collection<BgpPeerConfig> configs) {
        Set<String> neighbors = configs.stream()
                .map(cfg -> cfg.process.virtualRouter.getRouter().getRouterName())
                .collect(Collectors.toSet());
        String description = configs.stream()
                .map(BgpPeerConfig::getDescription)
                .filter(Objects::nonNull)
                .map(des -> des.replace("*", ""))
                .map(des -> des.replace("xxx", ""))
                .filter(des -> !des.equals(""))
                .findFirst().orElse("");
        String position = getLocation(neighbors);
        return new ISP(ip, as, String.join(", ", position, description, String.join(", ", neighbors)));
    }

    static final Set<String> GZ = Arrays.stream(new String[] {
            // GuangZhou dynamic PRs
            "7f6f28", "8ab2ec",
            // GuangZhou static PRs
            "958e33", "f034f3",
            // GuangZhou IDRs
            "5d20d8", "e09e84",
            // GuangZhou Cores
            "a20ff3", "d0c6bf",
            // GuangZhou DRs
            "73d604", "f71a57",
            // GuangZhou BRs
            "a847cc", "9ea744",
            "07f807", "5205c3", "de4bc0", "f605dc", "215796", "341839", "4901aa", "7e6556", "21562a", "b4d416",
            "2acbc6", "f2cce4"
    }).collect(Collectors.toSet());
    static final Set<String> BJ = Arrays.stream(new String[] {
            // BeiJing dynamic PRs
            "f309fa", "eecab6",
            // BeiJing static PRs
            "1e93fa", "9f1bc0",
            // BeiJing IDRs
            "07889c", "6edfa7",
            // BeiJing Cores
            "26d466", "d2be75",
            // BeiJing DRs
            "10f21d", "330d9c",
            // BeiJing BRs
            "d64f62", "34630b",
            // BeiJing IRR
            "7fef25",
            "c84a22", "f9467b", "b51290", "925abd", "a5d588", "82e74f", "2feabb", "4a6def", "958ea2",
            "b8bbc0", "346971"
    }).collect(Collectors.toSet());
    static final Set<String> SH = Arrays.stream(new String[] {
            // ShangHai dynamic PRs
            "71d266", "54dffa",
            // ShangHai BRs
            "04ad31", "909155",
            // ShangHai DRs
            "964628", "bb491b",
            "e95672", "d0940f", "1eea48", "862940", "6fa63c",
            "88383b", "2cbccb",

            "a124a9", "daf7dc",
            "bb46eb", "876da0",
            "7da922", "7da747",
            "34b0b0", "e13877"
    }).collect(Collectors.toSet());
    static final Set<String> GA = Arrays.stream(new String[] {
            // GuiAn dynamic PRs
            "abe2bb", "c80564",
            // GuiAn static PRs
            "c8065c", "abeee8",
            // GuiAn IDRs
            "abebc0", "c80bc0",
            // GuiAn DRs
            "abe8de", "c80124",
            "c80267", "abe4b9", "abe8fb", "c80dd9", "abe66a", "c80379",
            // GuiAn BRs
            "abed3f", "c80ddc"
    }).collect(Collectors.toSet());
    static final HashMap<Set<String>, String> LOCATIONS = new HashMap<>();
    static {
        LOCATIONS.put(GZ, "GZ");
        LOCATIONS.put(BJ, "BJ");
        LOCATIONS.put(SH, "SH");
        LOCATIONS.put(GA, "GA");
    }

    static String getLocation(Collection<String> neighbors) {
        for (Map.Entry<Set<String>, String> entry : LOCATIONS.entrySet()) {
            Set<String> common = new HashSet<>(neighbors);
            common.retainAll(entry.getKey());
            if (!common.isEmpty()) {
                return entry.getValue();
            }
        }

        List<String> regions = ImmutableList.of("bj", "dg", "ga", "gy", "gz", "hz", "sh", "sz", "ulanqab");
        for (String region : regions) {
            if (neighbors.stream().allMatch(nbr -> nbr.contains(region))) return region;
        }

        return "unknown";
    }

    static String getLocation(String... neighbors) {
        return getLocation(Sets.newHashSet(neighbors));
    }
}
