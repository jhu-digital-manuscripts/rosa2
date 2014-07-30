package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class BookImage implements IsSerializable {

    private String id;
    private int width;
    private int height;
    private boolean isMissing;

    public BookImage() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public void setMissing(boolean isMissing) {
        this.isMissing = isMissing;
    }

    // TODO equals/hashCode
}
