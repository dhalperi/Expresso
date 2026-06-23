package util;

import main.ExpressoLogger;
import org.junit.Test;

public class CombinationUtilTest {

    @Test
    public void numItems() {
        TimeUtil timer = new TimeUtil();
        long num = CombinationUtil.numItems(700, 6);
        System.out.println(num);
        timer.end(ExpressoLogger.LEVEL.INFO, "numItems finish");
    }
}
