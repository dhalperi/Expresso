package datamodel.acl;

import java.util.LinkedList;
import java.util.List;

/**
 * Huawei uses traffic policy to filter packets on interfaces.
 */
public class Acl {
    public final static Acl PERMIT_ALL = new Acl("PERMIT_ALL");

    String _name;
    List<AclLine> _lines;

    public Acl(String name) {
        _name = name;
        _lines = new LinkedList<>();
    }

    public Acl(String name, List<AclLine> lines) {
        _name = name;
        _lines = lines;
    }

    public String getName() {
        return _name;
    }

    public List<AclLine> getLines() {
        return _lines;
    }

    boolean used = false;

    public void setUsed(boolean used) {
        this.used = used;
    }
}
