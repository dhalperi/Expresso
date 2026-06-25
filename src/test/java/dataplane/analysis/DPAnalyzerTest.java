package dataplane.analysis;

import propertycheckers.JsonPrinter;
import bdd.BddManager;
import com.google.gson.stream.JsonWriter;
import controlplane.process.bgp.ISP;
import datamodel.Range;
import datamodel.ipv4.Ip;
import datamodel.ipv4.Prefix;
import datamodel.ipv4.PrefixRange;
import javafx.util.Pair;
import main.Controller;
import org.junit.Ignore;
import org.junit.Test;
import util.BddUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static main.Controller.bddManager;

public class DPAnalyzerTest {

    static Path outputBase = Paths.get("figures/bdd/dp_test");

    static Path output;
    static JsonWriter jw;
    static {
        try {
            output = outputBase.resolve("port-predicates.json");
            jw = new JsonWriter(new FileWriter(output.toFile()));
            jw.setIndent(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void computeFwdBDD(List<Pair<Integer, String>> fib) throws IOException {
        HashMap<String, Integer> fwdBDDs = new HashMap<>();
        int matched = 0;
        int intersect, oldP, newP;
        for (int length = 32; length >= 0; length--) {
            int lengthBdd = bddManager.getBddPrefixWrapper().encodeLength(length);
            List<Integer> list = new LinkedList<>();
            list.add(matched);
            for (Pair<Integer, String> fibEntry : fib) {
                intersect = bddManager.and(lengthBdd, fibEntry.getKey());
                if (intersect == 0) continue;
                intersect = bddManager.getBddPrefixWrapper().eraseLength(intersect, length);
                intersect = bddManager.getBddEnvcWrapper().transformCpEnvc2DpEnvc(intersect, length);
                intersect = bddManager.minus1(intersect, matched);
                if (intersect != 0) {
                    String intf = fibEntry.getValue();
                    oldP = fwdBDDs.getOrDefault(intf, 0);
                    newP = bddManager.or(oldP, intersect);
                    fwdBDDs.put(intf, newP);
                    list.add(intersect);
                }
            }
            matched = BddUtil.orInBatch(bddManager.getBDD(), list);
        }
        jw.beginObject();
        for (Map.Entry<String, Integer> fwdBdd : fwdBDDs.entrySet()) {
            bddManager.getBDD().printDot(outputBase.resolve(fwdBdd.getKey()).toString(), fwdBdd.getValue());
            jw.name(fwdBdd.getKey());
            JsonPrinter.printPrefixRange(jw, fwdBdd.getValue());
        }
        jw.endObject();
    }

    PrefixRange pr1, pr2;
    ISP isp1, isp2, isp3;
    int n1, n2, n3;

    public void init() {
        Controller.pushBDDManager(new BddManager());
        pr1 = PrefixRange.of(Prefix.of("128.0.0.0/2"), new Range<>(2));
        pr2 = PrefixRange.of(Prefix.of("128.0.0.0/2"), new Range<>(2, 32));
        isp1 = new ISP(Ip.of("10.0.0.1"), 1000, "C1");
        isp2 = new ISP(Ip.of("10.0.0.2"), 2000, "C2");
        isp3 = new ISP(Ip.of("10.0.0.3"), 3000, "C3");
        n1 = bddManager.getBddEnvcWrapper().addIspVar(isp1);
        n2 = bddManager.getBddEnvcWrapper().addIspVar(isp2);
        n3 = bddManager.getBddEnvcWrapper().addIspVar(isp3);
    }

    // Pre-existing failure (unrelated to the Batfish migration): the static initializer writes
    // debug JSON to figures/bdd/dp_test, which does not exist, so the FileWriter throws and the
    // static `jw` stays null, NPE-ing both tests.
    @Ignore("Pre-existing failure: static JsonWriter init writes to a missing directory; jw is null")
    @Test
    public void computeFwdBDD() throws IOException {
        init();

        jw.beginObject();

        List<Pair<Integer, String>> fib1 = new LinkedList<>();
        fib1.add(new Pair<>(pr1.toBdd(), "C1_RR"));
        fib1.add(new Pair<>(bddManager.and(bddManager.not(pr1.toBdd()), n1), "C1_ISP1"));
        fib1.add(new Pair<>(bddManager.and(bddManager.not(pr2.toBdd()), bddManager.and(bddManager.not(n1), n3)), "C1_C3"));
        jw.name("C1");
        computeFwdBDD(fib1);

        List<Pair<Integer, String>> fib2 = new LinkedList<>();
        fib2.add(new Pair<>(pr1.toBdd(), "C2_RR"));
        fib2.add(new Pair<>(bddManager.and(bddManager.not(pr2.toBdd()), n2), "C2_ISP2"));
        fib2.add(new Pair<>(bddManager.and(bddManager.not(pr2.toBdd()), bddManager.and(bddManager.not(n2), n3)), "C2_C3"));
        jw.name("C2");
        computeFwdBDD(fib2);

        List<Pair<Integer, String>> fib3 = new LinkedList<>();
        fib3.add(new Pair<>(pr1.toBdd(), "C3_RR"));
        fib3.add(new Pair<>(bddManager.and(bddManager.not(pr2.toBdd()), n3), "C3_ISP3"));
        fib3.add(new Pair<>(bddManager.and(bddManager.not(pr1.toBdd()), n1), "C3_C1"));
        jw.name("C3");
        computeFwdBDD(fib3);

        List<Pair<Integer, String>> fib4 = new LinkedList<>();
        fib4.add(new Pair<>(pr1.toBdd(), "RR_RR"));
        fib4.add(new Pair<>(bddManager.and(bddManager.not(pr1.toBdd()), n1), "RR_C2"));
        fib4.add(new Pair<>(bddManager.and(bddManager.not(pr2.toBdd()), n3), "RR_C3"));
        jw.name("RR");
        computeFwdBDD(fib4);

        List<Pair<Integer, String>> fib5 = new LinkedList<>();
        fib5.add(new Pair<>(pr1.toBdd(), "ISP3_C3"));
        fib5.add(new Pair<>(bddManager.and(bddManager.not(pr1.toBdd()), n1), "ISP3_C3"));
        jw.name("ISP3");
        computeFwdBDD(fib5);

        jw.endObject();
        jw.close();
    }

    @Ignore("Pre-existing failure: static JsonWriter init writes to a missing directory; jw is null")
    @Test
    public void computeBR2FwdBDD() throws IOException {
        init();

        jw.beginObject();

        List<Pair<Integer, String>> fib = new LinkedList<>();
        fib.add(new Pair<>(pr1.toBdd(), "BR2_DR1"));
        fib.add(new Pair<>(bddManager.and(bddManager.minus(pr2.toBdd(), pr1.toBdd()), n2), "BR2_ISP2"));
        fib.add(new Pair<>(bddManager.and(bddManager.minus(pr2.toBdd(), pr1.toBdd()), bddManager.minus(n3, n2)), "BR2_BR3"));
        for (Pair<Integer, String> entry : fib) {
            bddManager.getBDD().printDot(outputBase.resolve("fib_" + entry.getValue()).toString(), entry.getKey());
        }
        jw.name("BR2");
        computeFwdBDD(fib);

        System.out.println(bddManager.getBDD().numberOfVariables());

        jw.endObject();
        jw.close();
    }
}