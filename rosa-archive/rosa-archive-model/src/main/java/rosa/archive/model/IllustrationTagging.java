package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Information about zero or more illustrations.
 */
public class IllustrationTagging implements HasId, Iterable<Illustration>, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<Illustration> data;

    public IllustrationTagging() {
        this.data = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public int size() {
        return data.size();
    }

    /**
     *
     * @param index
     *          numeric index of illustration
     * @return
     *          illustration data at specified index
     */
    public Illustration getIllustrationData(int index) {
        return data.get(index);
    }

    public void addIllustrationData(Illustration data) {
        this.data.add(data);
    }

    /**
     *
     * @param illustrationId
     *          the ID of the illustration in question
     * @return
     *          numerical index of requested illustration, or -1 if illustration is not present
     */
    public int getIndexOfIllustration(String illustrationId) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(illustrationId)) {
                return i;
            }
        }
        return -1;
    }

    
    /**
     * @return indices of Illustrations on an image in a book
     */
    public List<Integer> findImageIndices(Book book, String image_id) {
        List<Integer> result = new ArrayList<Integer>();

        for (int i = 0; i < data.size(); i++) {
            String s = book.guessImageName(data.get(i).getPage());

            if (s != null && s.equals(image_id)) {
                result.add(i);
            }
        }

        return result;
    }
    
    @Override
    public Iterator<Illustration> iterator() {
        return new Iterator<Illustration>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < data.size();
            }

            @Override
            public Illustration next() {
                return getIllustrationData(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove items!");
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllustrationTagging)) return false;

        IllustrationTagging tagging = (IllustrationTagging) o;

        if (data != null ? !data.equals(tagging.data) : tagging.data != null) return false;
        if (id != null ? !id.equals(tagging.id) : tagging.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IllustrationTagging{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
