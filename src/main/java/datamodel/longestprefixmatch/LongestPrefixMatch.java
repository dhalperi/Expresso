package datamodel.longestprefixmatch;

import datamodel.ipv4.Ip;
import main.Controller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LongestPrefixMatch {
    /**
     * @return one result of longest prefix match
     */
    public static <T extends LongestPrefixMatchItem> T longestPrefixMatch(Collection<T> items, Ip ip) {
        int length = 32;
        while (length >= 0) {
            Optional<T> optional = match(items, ip, length);
            if (optional.isPresent()) {
                return optional.get();
            }
            length--;
        }
        return null;
    }

    public static <T extends LongestPrefixMatchItem> Optional<T> match(Collection<T> items, Ip ip, int length) {
        return items.stream()
                .filter(item -> {
                    int ipBdd = Controller.bddManager.getBddPrefixWrapper().encodeIp(ip);
                    int tmp0 = Controller.bddManager.getBddPrefixWrapper().encodeLength(length);
                    int tmp1 = Controller.bddManager.and(ipBdd, tmp0);
                    int tmp2 = Controller.bddManager.and(item.getPrefixesBdd(), tmp1);
                    return tmp1 == tmp2;
                })
                .findFirst();
    }

    /**
     * @return all results of longest prefix match
     */
    public static <T extends LongestPrefixMatchItem> List<T> longestPrefixMatches(Collection<T> items, Ip ip) {
        int length = 32;
        while (length >= 0) {
            List<T> list = matches(items, ip, length);
            if (!list.isEmpty()) {
                return list;
            }
            length--;
        }
        return null;
    }

    public static <T extends LongestPrefixMatchItem> List<T> matches(Collection<T> items, Ip ip, int length) {
        return items.stream()
                .filter(item -> {
                    int ipBdd = Controller.bddManager.getBddPrefixWrapper().encodeIp(ip);
                    int tmp0 = Controller.bddManager.getBddPrefixWrapper().encodeLength(length);
                    int tmp1 = Controller.bddManager.and(ipBdd, tmp0);
                    int tmp2 = Controller.bddManager.and(item.getPrefixesBdd(), tmp1);
                    return tmp1 == tmp2;
                })
                .collect(Collectors.toList());
    }
}
