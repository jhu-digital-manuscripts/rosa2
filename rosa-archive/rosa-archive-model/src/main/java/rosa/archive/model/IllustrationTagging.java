package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Information about zero or more illustrations.
 */
public class IllustrationTagging implements Iterable<IllustrationTaggingData>, IsSerializable {

    private List<IllustrationTaggingData> data;

    public IllustrationTagging() {
        this.data = new ArrayList<>();
    }

    public int getNumberOfIllustrations() {
        return data.size();
    }

    public IllustrationTaggingData getIllustrationData(int index) {
        return data.get(index);
    }

    public void addIllustrationData(IllustrationTaggingData data) {
        this.data.add(data);
    }

    public int getIndexOfIllustration(String illustrationId) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(illustrationId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Iterator<IllustrationTaggingData> iterator() {
        return new Iterator<IllustrationTaggingData>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < data.size();
            }

            @Override
            public IllustrationTaggingData next() {
                return getIllustrationData(index++);
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
