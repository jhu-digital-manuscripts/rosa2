package rosa.website.viewer.client.fsiviewer;

public enum FSIViewerType {
    PAGES("fsipages", "pages.fsi"), // Book view (show openings)
    SHOWCASE("fsishowcase", "showcase.fsi"); // Thumbnails (Also display single image)

    private String viewer_id;
    private String xml_id;

    FSIViewerType(String viewer_id, String xml_id) {
        this.viewer_id = viewer_id;
        this.xml_id = xml_id;
    }

    public String getViewerId() {
        return viewer_id;
    }

    public String getXmlId() {
        return xml_id;
    }
}
