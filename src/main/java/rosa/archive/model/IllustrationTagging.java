package rosa.archive.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Illustration tagging data for a book, containing information about
 * zero or more illustrations and their page locations.
 */
public class IllustrationTagging implements HasId, Iterable<Illustration> {

    private String id;
    private List<Illustration> data;

    /**
     * Creates an empty IllustrationTagging.
     */
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

    /**
     * Returns the number of illustrations.
     *
     * @return the illustration count
     */
    public int size() {
        return data.size();
    }

    /**
     * Returns the illustration at the specified index.
     *
     * @param index the zero-based index
     * @return the illustration data
     */
    public Illustration getIllustrationData(int index) {
        return data.get(index);
    }

    /**
     * Adds illustration data.
     *
     * @param illustration the illustration to add
     */
    public void addIllustrationData(Illustration illustration) {
        this.data.add(illustration);
    }

    /**
     * Returns the index of the illustration with the given id.
     *
     * @param illustrationId the illustration id to find
     * @return the index, or -1 if not found
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
     * Finds the indices of illustrations on a specific image in a book.
     *
     * @param book    the book containing the illustrations
     * @param imageId the image identifier
     * @return list of illustration indices matching the image
     */
    public List<Integer> findImageIndices(Book book, String imageId) {
        var result = new ArrayList<Integer>();
        for (int i = 0; i < data.size(); i++) {
            String imageName = book.guessImageName(data.get(i).getPage());
            if (imageName != null && imageName.equals(imageId)) {
                result.add(i);
            }
        }
        return result;
    }

    @Override
    public Iterator<Illustration> iterator() {
        return data.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllustrationTagging that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data);
    }

    @Override
    public String toString() {
        return "IllustrationTagging{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
