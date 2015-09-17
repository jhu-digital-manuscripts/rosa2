package rosa.website.model.view;

import rosa.archive.model.BookImage;
import rosa.archive.model.BookScene;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;

import java.io.Serializable;
import java.util.Map;

/**
 * Container for book model objects that are relevant to the
 * FSI flash viewer.
 */
public class FSIViewerModel implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Builder {
        public static Builder newBuilder() {
            return new Builder();
        }

        private String title;
        private Permission permission;
        private ImageList images;
        private Map<String, String> transcriptionMap;
        private IllustrationTagging illustrationTagging;
        private Map<String, String> illustrationTitles;
        private NarrativeTagging narrativeTagging;
        private NarrativeSections narrativeSections;

        private Builder() {}

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        public Builder images(ImageList images) {
            this.images = images;
            return this;
        }

        public Builder transcriptions(Map<String, String> transcriptionMap) {
            this.transcriptionMap = transcriptionMap;
            return this;
        }

        public Builder illustrationTagging(IllustrationTagging illustrationTagging) {
            this.illustrationTagging = illustrationTagging;
            return this;
        }

        public Builder illustrationTitles(Map<String, String> illustrationTitles) {
            this.illustrationTitles = illustrationTitles;
            return this;
        }

        public Builder narrativeTagging(NarrativeTagging narrativeTagging) {
            this.narrativeTagging = narrativeTagging;
            return this;
        }

        public Builder narrativeSections(NarrativeSections sections) {
            this.narrativeSections = sections;
            return this;
        }

        public FSIViewerModel build() {
            return new FSIViewerModel(title, permission, images, transcriptionMap, illustrationTagging,
                    illustrationTitles, narrativeTagging, narrativeSections);
        }
    }

    private String title;
    private Permission permission;
    private ImageList images;
    private Map<String, String> transcriptionMap;
    private IllustrationTagging illustrationTagging;
    private Map<String, String> illustrationTitles;
    private NarrativeTagging narrativeTagging;
    private NarrativeSections narrativeSections;

    private boolean needsRV;

    /** No-arg constructor for GWT RPC serialization */
    @SuppressWarnings("unused")
    FSIViewerModel() {}

    /**
     *
     * @param title display title
     * @param permission permission statements, includes other languages if applicable
     * @param images image list
     * @param transcriptionMap transcriptions, separated by page, if available
     * @param illustrationTagging illustration tagging
     * @param illustrationTitles illustration titles
     * @param narrativeTagging narrative tagging
     * @param narrativeSections narrative sections
     */
    FSIViewerModel(String title, Permission permission, ImageList images, Map<String, String> transcriptionMap,
                          IllustrationTagging illustrationTagging, Map<String, String> illustrationTitles,
                          NarrativeTagging narrativeTagging, NarrativeSections narrativeSections) {
        this.title = title;
        this.permission = permission;
        this.images = images;
        this.transcriptionMap = transcriptionMap;
        this.illustrationTagging = illustrationTagging;
        this.illustrationTitles = illustrationTitles;
        this.narrativeTagging = narrativeTagging;
        this.narrativeSections = narrativeSections;

        this.needsRV = false;
        if (images != null && images.getImages() != null) {
            for (BookImage img : images.getImages()) {
                String name = img.getName();
                if (name.endsWith("r") || name.endsWith("R") || name.endsWith("v") || name.endsWith("V")) {
                    this.needsRV = true;
                    break;
                }
            }
        }
    }

    public String getTitle() {
        return title;
    }

    public Permission getPermission() {
        return permission;
    }

    public ImageList getImages() {
        return images;
    }

    /**
     * Manuscripts will often name pages according to folio number plus a side
     * designation, 'r' or 'v' standing for recto or verso of the folio. This is
     * not always the case, however. Some books will use simple page numbers, like
     * those found in a book, where each page side will represent distinct numbers.
     *
     * First four pages:
     * EX1 - 1r, 1v, 2r, 2v
     * EX2 - 1, 2, 3, 4
     *
     * @return does this book require page names that end in 'r' or 'v'?
     */
    public boolean imagesNeedRV() {
        return needsRV;
    }

    public String getTranscription(String page) {
        if (transcriptionMap != null) {
            return transcriptionMap.get(page);
        } else {
            return null;
        }
    }

    public IllustrationTagging getIllustrationTagging() {
        return illustrationTagging;
    }

    public Map<String, String> getIllustrationTitles() {
        return illustrationTitles;
    }

    public NarrativeTagging getNarrativeTagging() {
        return narrativeTagging;
    }

    public NarrativeSections getNarrativeSections() {
        return narrativeSections;
    }

    public boolean hasTranscription(String page) {
        return transcriptionMap != null && transcriptionMap.containsKey(page);
    }

    public boolean hasIllustrationTagging(String page) {
        if (illustrationTagging != null) {
            for (int i = 0; i < illustrationTagging.size(); i++) {
                if (illustrationTagging.getIllustrationData(i).getPage().equals(page)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasNarrativeTagging(String page) {
        if (narrativeTagging != null) {
            for (BookScene scene : narrativeTagging.getScenes()) {
                if (scene.getStartPage().compareToIgnoreCase(page) <= 0 &&
                        scene.getEndPage().compareToIgnoreCase(page) >= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FSIViewerModel that = (FSIViewerModel) o;

        if (permission != null ? !permission.equals(that.permission) : that.permission != null) return false;
        if (images != null ? !images.equals(that.images) : that.images != null) return false;
        if (transcriptionMap != null ? !transcriptionMap.equals(that.transcriptionMap) : that.transcriptionMap != null)
            return false;
        if (illustrationTagging != null ? !illustrationTagging.equals(that.illustrationTagging) : that.illustrationTagging != null)
            return false;
        if (narrativeTagging != null ? !narrativeTagging.equals(that.narrativeTagging) : that.narrativeTagging != null)
            return false;
        return !(narrativeSections != null ? !narrativeSections.equals(that.narrativeSections) : that.narrativeSections != null);

    }

    @Override
    public int hashCode() {
        int result = permission != null ? permission.hashCode() : 0;
        result = 31 * result + (images != null ? images.hashCode() : 0);
        result = 31 * result + (transcriptionMap != null ? transcriptionMap.hashCode() : 0);
        result = 31 * result + (illustrationTagging != null ? illustrationTagging.hashCode() : 0);
        result = 31 * result + (narrativeTagging != null ? narrativeTagging.hashCode() : 0);
        result = 31 * result + (narrativeSections != null ? narrativeSections.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FSIViewerModel{" +
                "permission=" + permission +
                ", images=" + images +
                ", transcriptionMap=" + transcriptionMap +
                ", illustrationTagging=" + illustrationTagging +
                ", narrativeTagging=" + narrativeTagging +
                ", narrativeSections=" + narrativeSections +
                '}';
    }
}
