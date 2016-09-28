package rosa.pageturner.client.viewers;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FocusWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class FsiBase extends FocusWidget {

    private boolean debug;

    public FsiBase(Element elem) {
        super(elem);
    }

    protected native void console(String message) /*-{
        console.log("[Fsi] " + message);
    }-*/;

    public void setDebug(boolean debug) {
        this.debug = debug;
        Map<String, String> option = new HashMap<>();
        option.put("debug", String.valueOf(debug));
        setOptions(option);
    }

    protected boolean debug() {
        return debug;
    }

    public void setId(String id) {
        if (debug()) {
            console("Setting id: " + id);
        }

        getElement().setId(id);
    }

    public String getId() {
        return getElement().getId();
    }

    public void setOptions(Map<String, String> options) {
        for (Entry<String, String> option : options.entrySet()) {
            if (debug()) {
                console("Option: " + option.getKey() + "::" + option.getValue());
            }
            getElement().setAttribute(option.getKey(), option.getValue());
        }
    }

    public void setSize(int width, int height) {
        getElement().getStyle().setWidth(width, Unit.PX);
        getElement().getStyle().setHeight(height, Unit.PX);
    }

}
