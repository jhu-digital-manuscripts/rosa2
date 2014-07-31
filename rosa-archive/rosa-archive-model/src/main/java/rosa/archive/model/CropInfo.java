package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;

/**
 *
 */
public class CropInfo implements IsSerializable {

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

//    TODO expose underlying Map in this way?
//    public HashMap<String, CropData> getData() {
//        return data;
//    }
//
//    public void setData(HashMap<String, CropData> data) {
//        this.data = data;
//    }

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
