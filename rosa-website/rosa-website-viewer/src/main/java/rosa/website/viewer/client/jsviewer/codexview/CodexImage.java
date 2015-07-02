package rosa.website.viewer.client.jsviewer.codexview;

import rosa.website.viewer.client.jsviewer.dynimg.MasterImage;

public interface CodexImage extends MasterImage {
    String label();

    boolean missing();
}
