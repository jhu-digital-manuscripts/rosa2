package rosa.website.core.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface StaticResourceServiceAsync {
    void getStaticHtml(String name, String lang, AsyncCallback<String> cb);
}
