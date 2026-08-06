package rosa.archive.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Information about the cropping data of zero or more images.
 * Maps page identifiers to their crop data.
 */
public class CropInfo implements HasId, Iterable<CropData> {

    private String id;
    private Map<String, CropData> data;

    /**
     * Creates an empty CropInfo.
     */
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

    /**
     * Returns the crop data for the specified page.
     *
     * @param page the page identifier
     * @return the crop data, or {@code null} if not found
     */
    public CropData getCropDataForPage(String page) {
        return data.get(page);
    }

    /**
     * Adds crop data for a page.
     *
     * @param pageData the crop data to add (keyed by its id)
     */
    public void addCropData(CropData pageData) {
        data.put(pageData.getId(), pageData);
    }

    @Override
    public Iterator<CropData> iterator() {
        return data.values().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropInfo that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data);
    }

    @Override
    public String toString() {
        return "CropInfo{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
