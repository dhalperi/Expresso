package controlplane.network;

import main.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class InterfaceName implements Comparable<InterfaceName>, Serializable {
    private static final long serialVersionUID = -5651932486266221300L;

    final String _routerName;
    final String _interfaceName;

    public InterfaceName(String routerName, String interfaceName) {
        _routerName = routerName.toLowerCase(); // use lower case names to keep consistent with Batfish
        _interfaceName = interfaceName;
    }

    public String getRouterName() {
        return _routerName;
    }

    public String getInterfaceName() {
        return _interfaceName;
    }

    public String getFullName() {
        return _routerName + Configuration.ROUTER_INTF_CONNECTOR + _interfaceName;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfaceName interfaceName = (InterfaceName) o;
        return _routerName.equals(interfaceName._routerName) &&
               _interfaceName.equals(interfaceName._interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_routerName, _interfaceName);
    }

    @Override
    public int compareTo(@NotNull InterfaceName o) {
        return Comparator.comparing(InterfaceName::getRouterName).thenComparing(InterfaceName::getInterfaceName).compare(this, o);
    }
}
