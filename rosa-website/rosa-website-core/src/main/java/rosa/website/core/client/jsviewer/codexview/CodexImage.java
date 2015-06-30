package rosa.website.core.client.jsviewer.codexview;

import rosa.website.core.client.jsviewer.dynimg.MasterImage;

public interface CodexImage extends MasterImage {
    String label();

    boolean missing();
}
