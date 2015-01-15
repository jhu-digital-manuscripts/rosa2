package rosa.iiif.presentation.model.selector;

public class FragmentSelector implements Selector {

    private int x;
    private int y;
    private int width;
    private int height;

    public FragmentSelector() {}

    public FragmentSelector(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int[] getRegion() {
        return new int[] {x, y, width, height};
    }

    public void setRegion(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String context() {
        return "http://iiif.io/api/annex/openannotation/context.json";
    }

    @Override
    public String type() {
        return "iiif:ImageApiSelector";
    }

    @Override
    public String content() {
        return x + "," + y + "," + width + "," + height;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        result = prime * result + width;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FragmentSelector))
            return false;
        FragmentSelector other = (FragmentSelector) obj;
        if (height != other.height)
            return false;
        if (width != other.width)
            return false;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FragmentSelector [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }
}
