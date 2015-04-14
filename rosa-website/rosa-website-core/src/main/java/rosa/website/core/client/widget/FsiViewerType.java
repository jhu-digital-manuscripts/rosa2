package rosa.website.core.client.widget;

public enum FsiViewerType {
    PAGES("fsipages"), // Book view (show openings)
    SHOWCASE("fsishowcase"); // Thumbnails (Also display single image)

    private String viewer_id;

    FsiViewerType(String viewer_id) {
        this.viewer_id = viewer_id;
    }

    public String getViewerId() {
        return viewer_id;
    }
}
