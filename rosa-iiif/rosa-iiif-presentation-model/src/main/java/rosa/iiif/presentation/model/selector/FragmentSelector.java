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
}
