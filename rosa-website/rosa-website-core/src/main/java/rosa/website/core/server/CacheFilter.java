package rosa.website.core.server;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Lifted from the Rosa1 CacheFilter.
 * https://raw.githubusercontent.com/jhu-digital-manuscripts/rosa/master/rosa-website-common/src/main/java/rosa/gwt/common/server/CacheFilter.java
 */
public class CacheFilter implements Filter {
    private static final long YEAR = 31556926000l;
    private static final long WEEK = 604800000;

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpreq = (HttpServletRequest) request;

        String uri = httpreq.getRequestURI();

        if (!uri.contains(".nocache.")) {
            if (uri.contains(".cache.")) {
                HttpServletResponse httpresp = (HttpServletResponse) response;
                httpresp.setDateHeader("Expires", new Date().getTime() + YEAR);
            } else if (uri.contains("/data/") || uri.endsWith(".gif") || uri.endsWith(".jpg") || uri.endsWith(".png")) {
                HttpServletResponse httpresp = (HttpServletResponse) response;
                httpresp.setDateHeader("Expires", new Date().getTime() + WEEK);
            }
        }

        filterChain.doFilter(request, response);
    }

    public void destroy() {
    }

    public void init(FilterConfig config) throws ServletException {
    }
}
