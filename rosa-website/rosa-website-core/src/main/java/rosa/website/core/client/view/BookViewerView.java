package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface BookViewerView extends IsWidget {

    void setPermissionStatement(String perm);
    void setFlashViewer(String html, String viewerId);
    void useFlash(boolean useFlash);
}
