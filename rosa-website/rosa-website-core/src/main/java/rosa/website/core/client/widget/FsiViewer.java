package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

public class FsiViewer extends Composite {
    private FlowPanel root;
    private FlowPanel toolbar;

    public interface FSIPagesCallback {
        void pageChanged(int page);

        void imageInfo(String info);
    }

    public interface FSIShowcaseCallback {
        void imageSelected(int image);
    }

    public FsiViewer() {
        root = new FlowPanel();
        toolbar = new FlowPanel();

        root.setSize("100%", "100%");

        initWidget(root);
    }

    public void setToolbarVisibility(boolean visible) {
        toolbar.setVisible(visible);
    }

    public void clear() {
        root.clear();
    }

    public void setHtml(String html) {
        if (root.getWidgetCount() > 0) {
            clear();
        }
        HTML htmlWidget = new HTML(html);
        htmlWidget.setSize("100%", "100%");
        root.add(htmlWidget);
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

    public void fsipagesGotoImage(int image) {
        fsipagesSetVariable("newImageIndex", "" + image);
        fsipagesSetVariable("FSICMD", "GotoPage");
    }

    public void fsishowcaseSelectImage(int image) {
        fsishowcaseSetVariable("newImageIndex", "" + image);
        fsishowcaseSetVariable("FSICMD", "SelectImage");
    }
}
