package application.info;

import main.Controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AsInfo {
  public static final String TELECOM_REGEX = "4811|58466|23724|4134|4809|4812";
  public static final String UNICOM_REGEX = "4808|65123|136958|17622|4837";
  public static final String MOBILE_REGEX = "64950|24400|65270|9808";

  public static final String PROVIDER_REGEX =
      String.join("|", TELECOM_REGEX, UNICOM_REGEX, MOBILE_REGEX);

  public static boolean isProvider(long as) {
    return isType(as, PROVIDER_REGEX);
  }

  public static boolean isPeer(long as) {
    return !isProvider(as) && !isCustomer(as);
  }

  public static boolean isCustomer(long as) {
    return isInternalPrivateAS(as);
  }

  public static boolean isInternalPrivateAS(long as) {
    String regex =
        Controller.cpExecutor.bgpTopology.getInternalASNs().stream()
            .map(String::valueOf)
            .collect(Collectors.joining("|"));
    return isType(as, regex);
  }

  public static boolean isType(long as, String typeRegex) {
    Pattern pattern = Pattern.compile(typeRegex);
    Matcher matcher = pattern.matcher(String.valueOf(as));
    return matcher.matches();
  }
}
