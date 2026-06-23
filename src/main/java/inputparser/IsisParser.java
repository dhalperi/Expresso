package inputparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;
import controlplane.network.VirtualRouter;
import controlplane.process.isis.IsisRoutingProcess;
import org.batfish.datamodel.isis.IsisProcess;
import util.JsonUtil;

public class IsisParser {
  public static IsisRoutingProcess parseIsisProcess(VirtualRouter vrf, JsonObject jsonIsis) {
    if (jsonIsis == null) return null;

    IsisProcess isisProcess = null;
    try {
      isisProcess = JsonUtil.mapper.readValue(jsonIsis.toString(), IsisProcess.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return isisProcess == null ? null : new IsisRoutingProcess(vrf, isisProcess);
  }
}
