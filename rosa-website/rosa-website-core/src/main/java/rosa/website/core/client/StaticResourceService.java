package rosa.website.core.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * EX:
 * <code>
 * <servlet>
 *     <servlet-name>resourceService</servlet-name>
 *     <servlet-class>rosa.website.core.server.StaticResourceServiceImpl</servlet-class>
 * </servlet>
 *
 * <servlet-mapping>
 *     <servlet-name>resourceService</servlet-name>
 *     <url-pattern>/resource</url-pattern>
 * </servlet-mapping>
 * </code>
 */
@RemoteServiceRelativePath("resource")
public interface StaticResourceService extends RemoteService {
    String getStaticHtml(String name, String lang);
}
