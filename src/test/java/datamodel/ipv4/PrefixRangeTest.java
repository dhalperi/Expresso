package datamodel.ipv4;

import bdd.BddManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import datamodel.Range;
import main.Controller;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class PrefixRangeTest {
  @Test
  public void testToPrefixes() {
    Controller.pushBDDManager(new BddManager());

    /* prefixLength = rangeLowerBound = rangeUpperBound */
    PrefixRange prefixRange = PrefixRange.of(Prefix.of("128.0.0.0/32"), new Range<>(32));
    Collection<Prefix> prefixes =
        prefixRange.toPrefixes().stream().sorted().collect(Collectors.toList());
    Collection<Prefix> answer = Stream.of(Prefix.of("128.0.0.0/32")).collect(Collectors.toList());
    assertEquals(prefixes.size(), 1);
    assertEquals(prefixes, answer);

    /* prefixLength < rangeLowerBound = rangeUpperBound */
    prefixRange = PrefixRange.of(Prefix.of("128.0.0.0/29"), new Range<>(31));
    prefixes = prefixRange.toPrefixes().stream().sorted().collect(Collectors.toList());
    answer =
        Stream.of(
                Prefix.of("128.0.0.0/31"),
                Prefix.of("128.0.0.2/31"),
                Prefix.of("128.0.0.4/31"),
                Prefix.of("128.0.0.6/31"))
            .sorted()
            .collect(Collectors.toList());
    assertEquals(prefixes.size(), 4);
    assertEquals(prefixes, answer);

    /* prefixLength = rangeLowerBound < rangeUpperBound */
    prefixRange = PrefixRange.of(Prefix.of("128.0.0.0/31"), new Range<>(31, 32));
    prefixes = prefixRange.toPrefixes().stream().sorted().collect(Collectors.toList());
    answer =
        Stream.of(Prefix.of("128.0.0.0/31"), Prefix.of("128.0.0.0/32"), Prefix.of("128.0.0.1/32"))
            .sorted()
            .collect(Collectors.toList());
    assertEquals(prefixes.size(), 3);
    assertEquals(prefixes, answer);

    /* prefixLength < rangeLowerBound < rangeUpperBound */
    prefixRange = PrefixRange.of(Prefix.of("128.0.0.0/30"), new Range<>(31, 32));
    prefixes = prefixRange.toPrefixes().stream().sorted().collect(Collectors.toList());
    answer =
        Stream.of(
                Prefix.of("128.0.0.0/31"),
                Prefix.of("128.0.0.2/31"),
                Prefix.of("128.0.0.0/32"),
                Prefix.of("128.0.0.1/32"),
                Prefix.of("128.0.0.2/32"),
                Prefix.of("128.0.0.3/32"))
            .sorted()
            .collect(Collectors.toList());
    assertEquals(prefixes.size(), 6);
    assertEquals(prefixes, answer);
  }

  public static void run(Collection<Prefix> prefixes) {
    prefixes.stream().sorted().forEach(System.out::println);
    System.out.println("------------");

    int prefixesBdd = Controller.bddManager.getBddPrefixWrapper().encodePrefixes(prefixes);

    Controller.bddManager.getBDD().printDot("figures/tmp", prefixesBdd);

    Collection<PrefixRange> prefixRanges =
        Controller.bddManager.getBddPrefixWrapper().extractPrefixRanges(prefixesBdd).stream()
            .sorted()
            .collect(Collectors.toList());
    prefixRanges.forEach(System.out::println);
    System.out.println("------------");

    prefixRanges.stream()
        .flatMap(pr -> pr.toPrefixes().stream())
        .distinct()
        .sorted()
        .forEach(System.out::println);
  }

  @Test
  public void test1() {
    Controller.pushBDDManager(new BddManager());

    Prefix pfx1 = Prefix.of("84.137.254.63/22");
    Prefix pfx2 = Prefix.of("84.137.254.63/24");
    Prefix pfx3 = Prefix.of("84.137.254.69/32");
    Prefix pfx4 = Prefix.of("84.137.255.32/24");
    Prefix pfx5 = Prefix.of("84.137.253.32/24");
    Prefix pfx6 = Prefix.of("84.137.252.223/24");

    Collection<Prefix> prefixes =
        Stream.of(pfx1, pfx2, pfx3, pfx4, pfx5, pfx6).collect(Collectors.toList());

    run(prefixes);
  }

  @Test
  public void test2() {
    Controller.pushBDDManager(new BddManager());

    Prefix pfx1 = Prefix.of("13.160.81.224/255.255.240.0");
    Prefix pfx2 = Prefix.of("13.160.81.235/255.255.255.255");
    Prefix pfx3 = Prefix.of("13.160.81.246/255.255.255.255");
    Prefix pfx4 = Prefix.of("13.160.89.0/255.255.248.0");

    Collection<Prefix> prefixes =
        Stream.of(
                pfx1,
                //                pfx2,
                //                pfx3,
                pfx4)
            .collect(Collectors.toList());

    run(prefixes);
  }

  @Test
  public void test3() {
    Controller.pushBDDManager(new BddManager());

    Prefix pfx1 = Prefix.of("20.239.170.0/24");
    Prefix pfx2 = Prefix.of("20.239.171.0/24");
    Prefix pfx3 = Prefix.of("20.239.170.2/31");
    Prefix pfx4 = Prefix.of("20.239.170.0/31");
    Prefix pfx5 = Prefix.of("20.239.170.4/32");
    Prefix pfx6 = Prefix.of("20.239.170.5/32");

    Collection<Prefix> prefixes =
        Stream.of(pfx1, pfx2, pfx3, pfx4, pfx5, pfx6).collect(Collectors.toList());

    PrefixRange prefixRange = PrefixRange.of(Prefix.of("20.239.168.224/22"), new Range<>(23, 32));
    for (Prefix prefix : prefixes) {
      int tmp =
          Controller.bddManager.and(
              prefixRange.toBdd(),
              Controller.bddManager.getBddPrefixWrapper().encodePrefix(prefix));
      Controller.bddManager
          .getBddPrefixWrapper()
          .extractPrefixRanges(tmp)
          .forEach(System.out::println);
      System.out.println("------------");
    }

    //        run(prefixes);
  }
}
