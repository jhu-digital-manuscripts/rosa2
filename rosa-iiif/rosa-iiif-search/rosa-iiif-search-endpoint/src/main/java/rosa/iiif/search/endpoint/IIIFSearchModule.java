package rosa.iiif.search.endpoint;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFRequestParser;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuFSIImageIdMapper;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.search.core.IIIFLuceneSearchAdapter;
import rosa.iiif.search.core.IIIFSearchService;
import rosa.iiif.search.core.RosaIIIFSearchService;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class IIIFSearchModule extends ServletModule {
    private static final Logger logger = Logger.getLogger("");
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String FSI_SHARE_MAP_CONFIG_PATH = "/fsi-share-map.properties";

    @Override
    protected void configureServlets() {
        logger.info("Configuring IIIF Search servlet.");
        bind(IIIFRequestParser.class);
        bind(AnnotationTransformer.class);

        Names.bindProperties(binder(), loadProperties(SERVLET_CONFIG_PATH));

        serve("/*").with(IIIFSearchServlet.class);
        logger.info("Done configuring servlet.");
    }

    @Provides
    protected SearchService provideLuceneSearchService(@Named("search.index.path") String indexPath)
            throws IOException {
        return new LuceneSearchService(Paths.get(indexPath));
    }

    @Provides
    protected IIIFLuceneSearchAdapter getLuceneAdapter(AnnotationTransformer annotationTransformer,
                                                       Store archiveStore,
                                                       IIIFRequestFormatter presentationReqFormatter) {
        return new IIIFLuceneSearchAdapter(annotationTransformer, archiveStore, presentationReqFormatter);
    }

    @Provides
    protected Store provideStore(@Named("archive.path") String archivePath, SerializerSet serializers,
                              BookCollectionChecker collectionChecker, BookChecker bookChecker) {
        return new StoreImpl(serializers, bookChecker, collectionChecker,
                new FSByteStreamGroup(Paths.get(archivePath)));
    }

    @Provides
    protected IIIFSearchService provideIIIFSearchService(@Named("collection.id") String collectionId,
                                                      SearchService searchService, IIIFLuceneSearchAdapter adapter,
                                                      Store store) {
        logger.info("Creating IIIF search service. (collection: " + collectionId + ")");
        return new RosaIIIFSearchService(store, searchService, adapter, collectionId);
    }

    @Provides
    ImageIdMapper provideImageIdMapper(@Named("fsi.share.map") Map<String, String> fsi_share_map) {
        return new JhuFSIImageIdMapper(fsi_share_map);
    }

    @Provides
    IIIFRequestFormatter providePresentationRequestFormatter(@Named("iiif.pres.scheme") String scheme,
                                                             @Named("iiif.pres.host") String host,
                                                             @Named("iiif.pres.prefix") String prefix,
                                                             @Named("iiif.pres.port") int port) {
        return new IIIFRequestFormatter(scheme, host, prefix, port);
    }

    @Provides
    rosa.iiif.image.core.IIIFRequestFormatter provideImageRequestFormatter(@Named("iiif.image.scheme") String scheme,
                                                                           @Named("iiif.image.host") String host,
                                                                           @Named("iiif.image.prefix") String prefix,
                                                                           @Named("iiif.image.port") int port) {
        return new rosa.iiif.image.core.IIIFRequestFormatter(scheme, host, port, prefix);
    }

    @Provides
    @Named("fsi.share.map")
    protected Map<String, String> provideImageAlises() {
        Map<String, String> result = new HashMap<String, String>();

        Properties props = loadProperties(FSI_SHARE_MAP_CONFIG_PATH);

        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }

        return result;
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
