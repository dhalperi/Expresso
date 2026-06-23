package controlplane.process;

import controlplane.rib.Rib;

public interface Process {
    Rib<?> getRib();
}
