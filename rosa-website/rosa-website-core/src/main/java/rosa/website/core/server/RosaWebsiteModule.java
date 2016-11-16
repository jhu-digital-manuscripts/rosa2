package rosa.website.core.server;

import com.google.gwt.logging.server.RemoteLoggingServiceImpl;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;
import rosa.website.search.WebsiteLuceneMapper;
import rosa.website.viewer.server.FSISerializer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RosaWebsiteModule extends ServletModule {
    private static final Logger log = Logger.getLogger(RosaWebsiteModule.class.toString());

    private static final String FSI_MAP_NAME = "fsi-share-map.properties";

    private static final String PARAM_MODULE_NAME = "module.name";
    private static final String PARAM_ARCHIVE_PATH = "archive.path";
    private static final String PARAM_INDEX_PATH = "search.index.path";
    private static final String PARAM_COLLECTION_NAME = "collection.name";

    @Override
    protected void configureServlets() {
        log.info("Using module name: [" + moduleName() + "]");
        log.info("Using archive path: [" + archivePath() + "]");
        log.info("Using search index path: [" + searchIndexPath() + "]");
        log.info("On collection: [" + collectionName() + "]");

        bind(StoreProvider.class);
        bind(StoreAccessLayer.class).to(StoreAccessLayerImpl.class);
        bind(RemoteLoggingServiceImpl.class).in(Singleton.class);

        filter(buildUrlSegment("data")).through(CacheFilter.class);

        // Uncomment to enable remote logging
        //serve(buildUrlSegment("remote_logging")).with(RemoteLoggingServiceImpl.class);
        
        serve(buildUrlSegment("data")).with(ArchiveDataServiceImpl.class);
        serve(buildUrlSegment("fsi/*")).with(FSIDataServlet.class);
        serve(buildUrlSegment("search")).with(RosaSearchServiceImpl.class);

        log.info("Data RPC bound. [" + buildUrlSegment("data") + "]");
        log.info("FSI RPC bound. [" + buildUrlSegment("fsi/*") + "]");
        log.info("Search RPC bound. [" + buildUrlSegment("search") + "]");
    }

    @Provides @Named(PARAM_INDEX_PATH)
    public String searchIndexPath() {
        return getServletContext().getInitParameter(PARAM_INDEX_PATH);
    }

    @Provides @Named(PARAM_ARCHIVE_PATH)
    public String archivePath() {
        return getServletContext().getInitParameter(PARAM_ARCHIVE_PATH);
    }

    @Provides @Named(PARAM_MODULE_NAME)
    public String moduleName() {
        return getServletContext().getInitParameter(PARAM_MODULE_NAME);
    }

    @Provides @Named(PARAM_COLLECTION_NAME)
    public String collectionName() {
        return getServletContext().getInitParameter(PARAM_COLLECTION_NAME);
    }

    @Provides
    public SearchService searchService() {
        try {
            return new LuceneSearchService(Paths.get(searchIndexPath()), new WebsiteLuceneMapper());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to initialize search service.", e);
            return null;
        }
    }

    @Provides
    public FSISerializer fsiSerializer() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(FSI_MAP_NAME)) {
            Properties props = new Properties();
            props.load(in);

            Map<String, String> prop_map = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                prop_map.put(key, props.getProperty(key));
            }

            log.info("Loaded FSI share mapping. " + prop_map.toString());
            return new FSISerializer(prop_map);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to load " + FSI_MAP_NAME, e);
            return null;
        }
    }

    private String buildUrlSegment(String path) {
        return "/" + moduleName() + "/" + path;
    }

}
