package rosa.website.search.client;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;

/**
 * RPC service for searching the rosa archives.
 *
 * In order to use this service in a GWT client, an entry for
 * the service's servlet must be added to the apps web.xml. When this is updated,
 * a client can use the Async interface with callbacks.
 *
 * EX:
 * {@code
 * <servlet>
 *     <servlet-name>searchService</servlet-name>
 *     <servlet-class>rosa.website.core.server.RosaSearchServiceImpl</servlet-class>
 * </servlet>
 *
 * <servlet-mapping>
 *     <servlet-name>searchService</servlet-name>
 *     <url-pattern>/search</url-pattern>
 * </servlet-mapping>
 * }
 */
@RemoteServiceRelativePath("search")
public interface RosaSearchService {
    SearchResult search(Query query, SearchOptions options);
}
