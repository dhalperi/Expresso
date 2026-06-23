package datamodel.aspath;

import com.fasterxml.jackson.annotation.JsonValue;
import dk.brics.automaton.Automaton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public interface AsPathIntf {
  AsPathIntf removePrivateAs();

  @Nonnull
  AsPathIntf removeAsns(Collection<Long> asns);

  AsPathIntf append(List<AsSet> asSets);

  AsPathIntf overwrite(List<AsSet> asSets);

  @Nullable
  AsPathIntf allowAsLoop(long asn, int n);

  boolean containsAs(Long as);

  boolean containsOnly(Collection<Long> asns);

  @JsonValue
  String getAsString();

  Automaton getAsAutomaton();

  int length();
}
