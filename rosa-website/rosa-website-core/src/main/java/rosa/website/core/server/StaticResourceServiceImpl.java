package rosa.website.core.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.io.IOUtils;
import rosa.website.core.client.StaticResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StaticResourceServiceImpl extends RemoteServiceServlet implements StaticResourceService {
    private static final Logger logger = Logger.getLogger(StaticResourceServiceImpl.class.toString());
    private static final String HTML_EXT = ".html";

    private String prefix;

    @Override
    public void init() {
        prefix = "html/";
    }

    @Override
    public String getStaticHtml(String name, String lang) {
        String file_name = prefix
                + name + (lang != null && !lang.isEmpty() ? "_" + lang : "") + HTML_EXT;

        logger.fine("Trying to retrieve resource. [" + file_name + "]");

        String result = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(file_name)) {
            result = IOUtils.toString(in, "UTF-8");
            logger.log(Level.FINE, "Retrieved resource. [" + file_name + "]\n" + result);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to get resource. [" + file_name + "]", e);
        }

        return result;
    }
}
