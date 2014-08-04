package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Information about zero or more illustrations.
 */
public class IllustrationTagging implements Iterable<Illustration>, IsSerializable {

    private List<Illustration> data;

    public IllustrationTagging() {
        this.data = new ArrayList<>();
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

        IllustrationTagging that = (IllustrationTagging) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "IllustrationTagging{" +
                "data=" + data +
                '}';
    }
}
