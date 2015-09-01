package rosa.website.core.client;

public class Console {
    public static native void log(String message) /*-{
        console.log(message);
    }-*/;
}
