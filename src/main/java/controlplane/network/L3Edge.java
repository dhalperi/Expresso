package controlplane.network;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.io.Serializable;
import java.util.Objects;

public class L3Edge implements Serializable {
  private static final long serialVersionUID = -5376464008825444024L;

  private static final Table<InterfaceName, InterfaceName, L3Edge> CACHE = HashBasedTable.create();

  public static L3Edge of(InterfaceName port1, InterfaceName port2) {
    Direction direction = port1.compareTo(port2) < 0 ? Direction.FORWARD : Direction.BACKWARD;
    L3Edge l3Edge;
    if (!CACHE.contains(port1, port2)) {
      if (direction == Direction.BACKWARD) {
        InterfaceName tmp = port1;
        port1 = port2;
        port2 = tmp;
      }
      l3Edge = new L3Edge(port1, port2, direction);
      CACHE.put(port1, port2, l3Edge);
      CACHE.put(port2, port1, l3Edge);
    } else {
      l3Edge = CACHE.get(port1, port2);
      if (direction != l3Edge._direction) {
        l3Edge._direction = Direction.DUAL;
      }
    }
    return l3Edge;
  }

  public enum Direction {
    FORWARD,
    BACKWARD,
    DUAL
  }

  InterfaceName _port1;
  InterfaceName _port2;
  Direction _direction;

  private L3Edge(InterfaceName port1, InterfaceName port2, Direction direction) {
    _port1 = port1;
    _port2 = port2;
    _direction = direction;
  }

  public InterfaceName getSrc() {
    return _port1;
  }

  public InterfaceName getDst() {
    return _port2;
  }

  public boolean forward() {
    return _direction == Direction.FORWARD;
  }

  public boolean backward() {
    return _direction == Direction.BACKWARD;
  }

  public boolean dual() {
    return _direction == Direction.DUAL;
  }

  public InterfaceName getTheOtherEnd(InterfaceName oneEnd) {
    return _port1.equals(oneEnd) ? _port2 : (_port2.equals(oneEnd) ? _port1 : null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    L3Edge l3Edge = (L3Edge) o;
    return _port1.equals(l3Edge._port1)
        && _port2.equals(l3Edge._port2)
        && _direction.equals(l3Edge._direction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_port1, _port2, _direction);
  }

  @Override
  public String toString() {
    return "link{" + _port1.getFullName() + "," + _port2.getFullName() + "," + _direction + "}";
  }
}
