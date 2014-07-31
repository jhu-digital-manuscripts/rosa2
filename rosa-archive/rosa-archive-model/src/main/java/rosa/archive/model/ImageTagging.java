package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ImageTagging implements IsSerializable {

    private int numberOfImages;
    private List<ImageTaggingPage> pages;

    public ImageTagging() {
        this.pages = new ArrayList<>();
    }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public void setNumberOfImages(int numberOfImages) {
        this.numberOfImages = numberOfImages;
    }

//    TODO expose underlying list in this way?
//    public List<ImageTaggingPage> getPages() {
//        return pages;
//    }
//
//    public void setPages(List<ImageTaggingPage> pages) {
//        this.pages = pages;
//    }

    public ImageTaggingPage getPage(int index) {
        return pages.get(index);
    }

    public void addPage(ImageTaggingPage page) {
        pages.add(page);
    }

    public int getIndexOfPage(String pageId) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getId().equals(pageId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageTagging)) return false;

        ImageTagging that = (ImageTagging) o;

        if (numberOfImages != that.numberOfImages) return false;
        if (!pages.equals(that.pages)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = numberOfImages;
        result = 31 * result + pages.hashCode();
        return result;
    }
}
