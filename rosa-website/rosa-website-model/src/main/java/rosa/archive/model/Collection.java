package rosa.archive.model;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class Collection implements Iterable<CollectionEntry>, Serializable {
    private final List<CollectionEntry> entries;

    public Collection(List<CollectionEntry> entries) {
        this.entries = entries;
    }

    /**
     * @return number of entries in this collection
     */
    public int size() {
        return entries.size();
    }

    /**
     * @param index index of the entry
     * @return the entry at index
     */
    public CollectionEntry getEntry(int index) {
        return entries.get(index);
    }

    /**
     * @param id desired ID
     * @return entry with the given ID
     */
    public CollectionEntry getEntry(String id) {
        for (CollectionEntry entry : entries) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public Iterator<CollectionEntry> iterator() {
        return entries.iterator();
    }
}
