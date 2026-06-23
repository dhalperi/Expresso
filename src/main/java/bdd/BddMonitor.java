package bdd;

import main.Controller;

public class BddMonitor extends Thread {
    public boolean stop = false;

    @Override
    public void run() {
        while (!stop) {
            Controller.bddManager.showStats();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
