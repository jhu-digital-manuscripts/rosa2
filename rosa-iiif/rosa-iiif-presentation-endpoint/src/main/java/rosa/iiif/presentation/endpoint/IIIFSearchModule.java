package rosa.iiif.presentation.endpoint;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.Store;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFRequestParser;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.core.search.AnnotationLuceneMapper;
import rosa.iiif.presentation.core.search.IIIFLuceneSearchAdapter;
import rosa.iiif.presentation.core.search.IIIFSearchRequestFormatter;
import rosa.iiif.presentation.core.search.IIIFSearchService;
import rosa.iiif.presentation.core.search.RosaIIIFSearchService;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class IIIFSearchModule extends ServletModule {
    private static final Logger logger = Logger.getLogger("");
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";

    @Override
    protected void configureServlets() {
        logger.info("Configuring IIIF Search servlet.");
        bind(ArchiveNameParser.class);
        bind(IIIFRequestParser.class);
        bind(AnnotationTransformer.class);

        Names.bindProperties(binder(), loadProperties(SERVLET_CONFIG_PATH));

        // TODO not serving anything, must set to not conflict with iiif-presentation
//        serve("/*").with(IIIFSearchServlet.class);
        logger.info("Done configuring servlet.");
    }

    @Provides
    protected SearchService provideLuceneSearchService(@Named("search.index.path") String indexPath)
            throws IOException {
        return new LuceneSearchService(Paths.get(indexPath), new AnnotationLuceneMapper());
    }

    @Provides
    protected IIIFLuceneSearchAdapter getLuceneAdapter(AnnotationTransformer annotationTransformer,
                                                       Store archiveStore,
                                                       @Named("formatter.presentation") IIIFRequestFormatter reqFormatter) {
        return new IIIFLuceneSearchAdapter(annotationTransformer, archiveStore, reqFormatter);
    }

    @Provides
    protected IIIFSearchService provideIIIFSearchService(@Named("collection.id") String collectionId,
                                                         SearchService searchService,
                                                         IIIFLuceneSearchAdapter adapter,
                                                         Store store,
                                                         IIIFSearchRequestFormatter requestFormatter) {
        logger.info("Creating IIIF search service. (collection: " + collectionId + ")");
        return new RosaIIIFSearchService(store, searchService, adapter, collectionId, requestFormatter);
    }

    @Provides
    protected IIIFSearchRequestFormatter provideSearchRequestFormatter(@Named("iiif.search.scheme") String scheme,
                                                                       @Named("iiif.search.host") String host,
                                                                       @Named("iiif.search.prefix") String prefix,
                                                                       @Named("iiif.search.port") int port) {
        return new IIIFSearchRequestFormatter(scheme, host, prefix, port);
    }

    private Properties loadProperties(String path) {
        logger.info("Loading servlet properties [" + path + "]");
        Properties props = new Properties();

        try (InputStream in = getClass().getResourceAsStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }

        return props;
    }

    @Provides @Named("ignored.parameters")
    public String[] getIgnoredParams(@Named("search.ignored") String ignored) {
        if (ignored == null) {
            return new String[0];
        }

        return ignored.split(",");
    }

}
