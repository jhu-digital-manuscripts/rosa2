package rosa.website.core.server;

import com.google.gwt.logging.server.RemoteLoggingServiceImpl;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import rosa.website.viewer.server.FSIDataServlet;

import java.util.logging.Logger;

public class RosaWebsiteModule extends ServletModule {
    private static final Logger log = Logger.getLogger(RosaWebsiteModule.class.toString());

    private static final String PARAM_MODULE_NAME = "module.name";
    private static final String PARAM_ARCHIVE_PATH = "archive.path";

    @Override
    protected void configureServlets() {
        log.info("Using module name: [" + moduleName() + "]");
        log.info("Using archive path: [" + archivePath() + "]");

        bind(StoreProvider.class);
        bind(RemoteLoggingServiceImpl.class).in(Singleton.class);

        filter(buildUrlSegment("data")).through(CacheFilter.class);

        serve(buildUrlSegment("remote_logging")).with(RemoteLoggingServiceImpl.class);
        serve(buildUrlSegment("data")).with(ArchiveDataServiceImpl.class);
        serve(buildUrlSegment("fsi/*")).with(FSIDataServlet.class);

        log.info("Data RPC bound. [" + buildUrlSegment("data"));
        log.info("FSI RPC bound. [" + buildUrlSegment("fsi/*"));
    }

    private String buildUrlSegment(String path) {
        return "/" + moduleName() + "/" + path;
    }

    @Provides @Named(PARAM_ARCHIVE_PATH)
    public String archivePath() {
        return getServletContext().getInitParameter(PARAM_ARCHIVE_PATH);
    }

    @Provides @Named(PARAM_MODULE_NAME)
    public String moduleName() {
        return getServletContext().getInitParameter(PARAM_MODULE_NAME);
    }

}
