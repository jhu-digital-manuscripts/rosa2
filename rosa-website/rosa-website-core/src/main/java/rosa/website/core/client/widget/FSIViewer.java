package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

public class FSIViewer extends Composite {

    public interface FSIPagesCallback {
        void pageChanged(int page);

        void imageInfo(String info);
    }

    public interface FSIShowcaseCallback {
        void imageSelected(int image);
    }

    private SimplePanel viewer;

    private FSIViewerType type;

    /**  */
    public FSIViewer() {
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
    public void setHtml(String html, FSIViewerType type) {
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

    /**
     * @param cb FSIPagesCallback
     */
    public native void setupFSIPagesCallback(FSIPagesCallback cb) /*-{
         $wnd.fsipages_DoFSCommand = function (fsievent, args) {
             switch (fsievent) {
                 case "ImageInfo":
                     cb.@rosa.website.core.client.widget.FSIViewer.FSIPagesCallback::imageInfo(Ljava/lang/String;)(args);
                     break;

                 case "onPagesPageChanged":
                     cb.@rosa.website.core.client.widget.FSIViewer.FSIPagesCallback::pageChanged(I)(args);
                     break;
             }
         }
     }-*/;

    // Flash object must have fsishowcase id

    /**
     * @param cb FSIShowcaseCallback
     */
    public native void setupFSIShowcaseCallback(FSIShowcaseCallback cb) /*-{
        $wnd.fsishowcase_DoFSCommand = function (fsievent, args) {
            switch (fsievent) {
                case "ImageSelected":
                    cb.@rosa.website.core.client.widget.FSIViewer.FSIShowcaseCallback::imageSelected(I)(args);
                    break;
            }
        }
    }-*/;

    private native void setVariable(String name, String value, String viewerId) /*-{
        var fsiobj = $doc.getElementById(viewerId);
        if (viewerId == 'fsipages') {
            fsiobj = $doc.fsipages;
        } else if (viewerId == 'fsishowcase') {
            fsiobj = $doc.fsishowcase;
        }

        if (fsiobj) {
            fsiobj.SetVariable(name, value);
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
                    children[0].setAttribute('width', width);
                }
                if (height) {
                    children[0].setAttribute('height', height);
                }
            }
        }
    }-*/;

    /**
     * Tell the FSI pages widget to go to the specified page.
     *
     * @param image index
     */
    public void fsiViewerGoToImage(int image) {
        if (type == FSIViewerType.PAGES) {
            setVariable("newImageIndex", String.valueOf(image), FSIViewerType.PAGES.getViewerId());
            setVariable("FSICMD", "GotoPage", FSIViewerType.PAGES.getViewerId());
        }
    }

    /**
     * Tell the FSI Showcase that a page has been selected.
     *
     * @param image image index
     */
    public void fsiSelectImage(int image) {
        if (type == FSIViewerType.SHOWCASE) {
            setVariable("newImageIndex", String.valueOf(image), FSIViewerType.SHOWCASE.getViewerId());
            setVariable("FSICMD", "SelectImage", FSIViewerType.SHOWCASE.getViewerId());
        }
    }
}
