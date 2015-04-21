package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.logging.Logger;

public class FsiViewer extends Composite {
    private static final Logger logger = Logger.getLogger(FsiViewer.class.toString());

    public interface FSIPagesCallback {
        void pageChanged(int page);

        void imageInfo(String info);
    }

    public interface FSIShowcaseCallback {
        void imageSelected(int image);
    }

    private SimplePanel viewer;
    private FlowPanel toolbar;

    private FsiViewerType type;

    public FsiViewer() {
        FlowPanel root = new FlowPanel();
        viewer = new SimplePanel();
        toolbar = new FlowPanel();

        root.add(toolbar);
        root.add(viewer);
        root.setSize("100%", "100%");

        initWidget(root);
    }

    public void setToolbarVisibile(boolean visible) {
        toolbar.setVisible(visible);
    }

    public void clear() {
        viewer.clear();
        type = null;
    }

    public void setHtml(String html, FsiViewerType type) {
        this.type = type;

        HTML htmlWidget = new HTML(html);
        htmlWidget.setSize("100%", "100%");

        viewer.setWidget(htmlWidget);
    }

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

    private native void fsipagesSetVariable(String name, String val) /*-{
        var fsiobj = $doc.all ? $doc.getElementById('fsipages') : $doc.fsipages;

        if (fsiobj) {
            fsiobj.SetVariable(name, val);
        }
    }-*/;

    private native void fsishowcaseSetVariable(String name, String val) /*-{
        var fsiobj = $doc.all ? $doc.getElementById('fsishowcase') : $doc.fsishowcase;

        if (fsiobj) {
            fsiobj.SetVariable(name, val);
        }
    }-*/;

    private native void changeViewerDimension(String width, String height, String viewerId) /*-{
        var fsiobj = $doc.all ? $doc.getElementById('fsishowcase') : $doc.fsishowcase;

        if (fsiobj) {
            if (width) {
                fsiobj.setAttribute('width', width);
            }
            if (height) {
                fsiobj.setAttribute('height', height);
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

    public void fsipagesGotoImage(int image) {
        fsipagesSetVariable("newImageIndex", "" + image);
        fsipagesSetVariable("FSICMD", "GotoPage");
    }

    public void fsishowcaseSelectImage(int image) {
        fsishowcaseSetVariable("newImageIndex", "" + image);
        fsishowcaseSetVariable("FSICMD", "SelectImage");
    }
}
