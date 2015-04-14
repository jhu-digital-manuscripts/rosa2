package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface BrowseBookView extends IsWidget {

    void setPermissionStatement(String perm);
    void setFlashViewer(String html);
    void useFlash(boolean useFlash);
}
