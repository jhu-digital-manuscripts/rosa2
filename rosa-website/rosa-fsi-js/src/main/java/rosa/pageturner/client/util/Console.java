package rosa.pageturner.client.util;

public class Console {
    public static native void log(String message) /*-{
        console.log(message);
     }-*/;
}
