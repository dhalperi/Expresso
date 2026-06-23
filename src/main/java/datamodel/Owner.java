package datamodel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

public class Owner<I, O> {
    private final Multimap<I, O> owners;

    public Owner() {
        owners = HashMultimap.create();
    }

    public void add(I item, O owner) {
        owners.put(item, owner);
    }

    public Collection<O> get(I item) {
        return owners.get(item);
    }

    public Collection<I> keys() {
        return owners.keySet();
    }

    public Collection<Map.Entry<I, O>> entries() {
        return owners.entries();
    }

    public O getOne(I item) {
        if (owners.containsKey(item)) {
            return owners.get(item).iterator().next();
        }
        return null;
    }
}
