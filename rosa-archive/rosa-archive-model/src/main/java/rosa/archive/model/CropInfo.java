package rosa.archive.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Information about about the cropping data of zero or more images.
 */
public class CropInfo implements HasId, Iterable<CropData>, Serializable {
    static final long serialVersionUID = 1L;

    private HashMap<String, CropData> data;
    private String id;

    public CropInfo() {
        this.data = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public CropData getCropDataForPage(String page) {
        return data.get(page);
    }

    public void addCropData(CropData pageData) {
        data.put(pageData.getId(), pageData);
    }

    @Override
    public Iterator<CropData> iterator() {
        final Iterator<Entry<String, CropData>> datarator = data.entrySet().iterator();

        return new Iterator<CropData>() {
            @Override
            public boolean hasNext() {
                return datarator.hasNext();
            }

            @Override
            public CropData next() {
                return datarator.next().getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove item from this iterator.");
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropInfo)) return false;

        CropInfo info = (CropInfo) o;

        if (data != null ? !data.equals(info.data) : info.data != null) return false;
        if (id != null ? !id.equals(info.id) : info.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CropInfo{" +
                "data=" + data +
                ", id='" + id + '\'' +
                '}';
    }
}
