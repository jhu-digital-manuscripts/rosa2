package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

public class FsiViewer extends Composite {

    public interface FSIPagesCallback {
        void pageChanged(int page);

        void imageInfo(String info);
    }

    public interface FSIShowcaseCallback {
        void imageSelected(int image);
    }

    private SimplePanel viewer;

    private FsiViewerType type;

    /**  */
    public FsiViewer() {
        FlowPanel root = new FlowPanel();
        viewer = new SimplePanel();

        root.add(viewer);
        root.setSize("100%", "100%");

        initWidget(root);
    }

    /**
     * Clear contents of this viewer.
     */
    public void clear() {
        viewer.clear();
        type = null;
    }

    /**
     * Set HTML of the flash image viewer.
     *
     * @param html HTML of viewer
     * @param type type of viewer
     */
    public void setHtml(String html, FsiViewerType type) {
        this.type = type;

        HTML htmlWidget = new HTML(html);
        htmlWidget.setSize("100%", "100%");

        viewer.setWidget(htmlWidget);
    }

    /**
     * Resize the viewer.
     *
     * @param width in pixels
     * @param height in pixels
     */
    public void resize(String width, String height) {
        changeViewerDimension(width, height, type.getViewerId());
    }

    // Flash object must have fsipages id
    public native void setupFSIPagesCallback(FSIPagesCallback cb) /*-{
         $wnd.fsipages_DoFSCommand = function (fsievent, args) {
             switch (fsievent) {
                 case "ImageInfo":
                     cb.@rosa.website.core.client.widget.FsiViewer.FSIPagesCallback::imageInfo(Ljava/lang/String;)(args);
                     break;

                 case "onPagesPageChanged":
                     cb.@rosa.website.core.client.widget.FsiViewer.FSIPagesCallback::pageChanged(I)(args);
                     break;
             }
         }
     }-*/;

    // Flash object must have fsishowcase id
    public native void setupFSIShowcaseCallback(FSIShowcaseCallback cb) /*-{
        $wnd.fsishowcase_DoFSCommand = function (fsievent, args) {
            switch (fsievent) {
                case "ImageSelected":
                    cb.@rosa.website.core.client.widget.FsiViewer.FSIShowcaseCallback::imageSelected(I)(args);
                    break;
            }
        }
    }-*/;

    private native void setVariable(String name, String value, String viewerId) /*-{
        var fsiobj = $doc.getElementById(viewerId);


        if (fsiobj) {
            fsiobj.SetVariable(name, val);
        }
    }-*/;

    private native void changeViewerDimension(String width, String height, String viewerId) /*-{
        var fsiobj = $doc.getElementById(viewerId);

        if (fsiobj) {
            if (width) {
                fsiobj.setAttribute('width', width+'px');
            }
            if (height) {
                fsiobj.setAttribute('height', height+'px');
            }

            var children = $doc.getElementById(viewerId).getElementsByTagName('embed');
            if (children && children[0]) {
                if (width) {
                    children[0].setAttribute('width', width+'px');
                }
                if (height) {
                    children[0].setAttribute('height', height+'px');
                }
            }
        }
    }-*/;

    public void fsiViewerGoToImage(int image) {
        if (type == FsiViewerType.PAGES) {
            setVariable("newImageIndex", String.valueOf(image), FsiViewerType.PAGES.getViewerId());
            setVariable("FSICMD", "GotoPage", FsiViewerType.PAGES.getViewerId());
        }
    }

    public void fsiSelectImage(int image) {
        if (type == FsiViewerType.SHOWCASE) {
            setVariable("newImageIndex", String.valueOf(image), FsiViewerType.SHOWCASE.getViewerId());
            setVariable("FSICMD", "SelectImage", FsiViewerType.SHOWCASE.getViewerId());
        }
    }
}
