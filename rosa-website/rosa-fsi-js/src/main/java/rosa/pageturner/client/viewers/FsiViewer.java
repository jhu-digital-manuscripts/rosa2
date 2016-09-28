package rosa.pageturner.client.viewers;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import rosa.pageturner.client.util.Console;

import java.util.Map;
import java.util.Map.Entry;

public class FsiViewer extends FsiBase {

    public FsiViewer() {
        super(Document.get().createElement("fsi-viewer"));
        Console.log("[FsiViewer] creating new viewer.");
    }

    /**
     * Change current image
     *
     * @param imageId ID of image on FSI server (EX: rosa/Arsenal3339/Arsenal3339.001r.tif)
     */
    public void changeImage(String imageId) {
        if (debug()) {
            console("[" + getId() + "] Changing image -> " + imageId);
        }
        // Don't bother changing if current image is the same as desired image
        if (!getElement().getAttribute("src").equals(imageId)) {
            getElement().setAttribute("src", imageId);
            changeImage(getElement(), imageId);
        }
    }

    public void changeConfig(String pathToConfigFile, Map<String, String> params) {
        Console.log("[FsiViewer] changing config. " + pathToConfigFile + "\n" + params.toString());
        StringBuilder p = new StringBuilder("{");
        for (Entry<String, String> param : params.entrySet()) {
            p.append(param.getKey()).append(':').append(param.getValue()).append(',');
        }
        // Remove trailing comma and close JSON object
        p.deleteCharAt(p.length()).append('}');
        changeConfig(getElement(), pathToConfigFile, params.toString());
    }

    /**
     * Destroys the given FSI Viewer JS 360 object. You should destroy the instance
     * before you remove the object spins <div> tag from the DOM tree.
     */
    public void destroy() {
        if (debug()) {
            console("Destroying FSI Viewer instance.");
        }
        destroy(getElement());
    }

    /**
     * @return string containing the FSI Viewer JS software version.
     */
    public String getVersion() {
        return getVersion(getElement());
    }

    /**
     * Reset the viewer to the initial magnification and rotation.
     */
    public void resetView() {
        resetView(getElement());
    }

    private native void changeImage(Element elem, String imageId) /*-{
        var param = { fpxsrc: imageId };
        elem.changeImage(param);
    }-*/;

    private native void changeConfig(Element elem, String path, String params) /*-{
        elem.changeConfig(path, params);
    }-*/;

    private native void destroy(Element elem) /*-{
        elem.destroy();
    }-*/;

    private native String getVersion(Element elem) /*-{
        return elem.getVersion();
    }-*/;

    private native void resetView(Element elem) /*-{
        elem.resetView();
    }-*/;

}
