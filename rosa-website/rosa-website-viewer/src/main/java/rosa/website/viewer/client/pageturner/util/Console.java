package rosa.website.viewer.client.pageturner.util;

public class Console {
    public static native void log(String message) /*-{
        console.log(message);
     }-*/;
}
