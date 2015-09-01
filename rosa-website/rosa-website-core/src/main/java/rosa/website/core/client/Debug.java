package rosa.website.core.client;

public class Debug {
    public static native void console(String message) /*-{
        console.log(message);
    }-*/;
}
