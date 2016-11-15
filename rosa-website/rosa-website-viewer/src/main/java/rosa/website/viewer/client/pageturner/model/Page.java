package rosa.website.viewer.client.pageturner.model;

public class Page {
    public final String id;
    public final String label;
    public final boolean missing;

    public final int width;
    public final int height;

    public Page(String id, String label, boolean missing, int width, int height) {
        if (id == null) {
            throw new IllegalArgumentException("ID must be specified.");
        }
        this.id = id;
        this.label = label;
        this.missing = missing;
        this.width = width;
        this.height = height;
    }

    /**
     * @return aspect ratio = width / height
     */
    public double getAspectRatio() {
        return (double)width / height;
    }

    public int getScaledWidth(int desiredHeight) {
        return (int) (getAspectRatio() * desiredHeight);
    }

    public int getScaledHeight(int desiredWidth) {
        return  (int) (width / getAspectRatio());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page)) return false;

        Page page = (Page) o;

        if (missing != page.missing) return false;
        if (width != page.width) return false;
        if (height != page.height) return false;
        if (!id.equals(page.id)) return false;
        return label != null ? label.equals(page.label) : page.label == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (missing ? 1 : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Page{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", missing=" + missing +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
