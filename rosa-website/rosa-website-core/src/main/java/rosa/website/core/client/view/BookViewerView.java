package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.core.client.widget.FsiViewerType;

public interface BookViewerView extends IsWidget {

    void setPermissionStatement(String perm);
    void setFlashViewer(String html, FsiViewerType type);
    void useFlash(boolean useFlash);
    void onResize();
}
