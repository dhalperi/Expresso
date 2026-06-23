package main;

import bdd.BddManager;
import controlplane.network.CPExecutor;
import datamodel.ipv4.PrefixRange;
import dataplane.analysis.DPAnalyzer;
import main.storage.Storage;

import java.util.HashSet;
import java.util.LinkedList;

public class Controller {
  /* initialization */
  // ID = "" means single thread execution
  public static String ID = "";

  public static LinkedList<Storage> storageStack = new LinkedList<>();
  public static Storage storage;
  public static LinkedList<BddManager> bddManagerStack = new LinkedList<>();
  public static BddManager bddManager;
  public static LinkedList<CPExecutor> cpExecutorStack = new LinkedList<>();
  public static CPExecutor cpExecutor;
  public static LinkedList<DPAnalyzer> dpAnalyzerStack = new LinkedList<>();
  public static DPAnalyzer dpAnalyzer;
  public static LinkedList<HashSet<PrefixRange>> prefixSpaceStack = new LinkedList<>();
  public static HashSet<PrefixRange> prefixSpace;

  public static void pushStorage(Storage storage) {
    storageStack.push(storage);
    Controller.storage = storage;
    storage.init();
  }

  public static void pushBDDManager(BddManager bddManager) {
    bddManagerStack.push(bddManager);
    Controller.bddManager = bddManager;
  }

  public static void pushCPExecutor(CPExecutor cpExecutor) {
    cpExecutorStack.push(cpExecutor);
    Controller.cpExecutor = cpExecutor;
  }

  public static void pushDPAnalyzer(DPAnalyzer dpAnalyzer) {
    dpAnalyzerStack.push(dpAnalyzer);
    Controller.dpAnalyzer = dpAnalyzer;
  }

  public static void pushPrefixSpace(HashSet<PrefixRange> prefixSpace) {
    prefixSpaceStack.push(prefixSpace);
    Controller.prefixSpace = prefixSpace;
  }

  public static void popStorage() {
    if (!storageStack.isEmpty()) {
      storageStack.addLast(storageStack.pop());
      storage = storageStack.peekFirst();
      assert storage != null;
      storage.init();
    }
  }

  public static void popBDDManager() {
    if (!bddManagerStack.isEmpty()) {
      bddManagerStack.addLast(bddManagerStack.pop());
      bddManager = bddManagerStack.peekFirst();
    }
  }

  public static void popCPExecutor() {
    if (!cpExecutorStack.isEmpty()) {
      cpExecutorStack.addLast(cpExecutorStack.pop());
      cpExecutor = cpExecutorStack.peekFirst();
    }
  }

  public static void popDPAnalyzer() {
    if (!dpAnalyzerStack.isEmpty()) {
      dpAnalyzerStack.addLast(dpAnalyzerStack.pop());
      dpAnalyzer = dpAnalyzerStack.peekFirst();
    }
  }

  public static void popPrefixSpace() {
    if (!prefixSpaceStack.isEmpty()) {
      prefixSpaceStack.addLast(prefixSpaceStack.pop());
      prefixSpace = prefixSpaceStack.peekFirst();
    }
  }
}
