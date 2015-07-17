package rosa.website.core.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;

public interface RosaSearchServiceAsync {
    void search(Query query, SearchOptions options, AsyncCallback<SearchResult> cb);
}
