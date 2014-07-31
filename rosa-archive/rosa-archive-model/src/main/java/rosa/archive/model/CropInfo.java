package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Information about about the cropping data of zero or more images.
 */
public class CropInfo implements Iterable<CropData>, IsSerializable {

    private HashMap<String, CropData> data;

    public CropInfo() {
        this.data = new HashMap<>();
    }

    public CropData getCropDataForPage(String page) {
        return data.get(page);
    }

    public void addCropData(CropData pageData) {
        data.put(pageData.getId(), pageData);
    }

    @Override
    public Iterator<CropData> iterator() {
        final Iterator<Entry<String, CropData>> daterator = data.entrySet().iterator();

        return new Iterator<CropData>() {
            @Override
            public boolean hasNext() {
                return daterator.hasNext();
            }

            @Override
            public CropData next() {
                return daterator.next().getValue();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropInfo)) return false;

        CropInfo cropInfo = (CropInfo) o;

        if (data != null ? !data.equals(cropInfo.data) : cropInfo.data != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CropInfo{" +
                "data=" + data +
                '}';
    }
}
