package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.SimpleCachingStore;
import rosa.archive.core.SimpleStore;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.iiif.presentation.core.ArchiveIIIFPresentationService;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestParser;
import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuImageIdMapper;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.core.jhsearch.LuceneJHSearchService;
import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;

/**
 * The servlet is configured by iiif-servlet.properties.
 */

public class IIIFPresentationServletModule extends ServletModule {
    private static final Logger LOG = Logger.getLogger(IIIFPresentationServletModule.class.toString());
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String FSI_SHARE_MAP_CONFIG_PATH = "/fsi-share-map.properties";
    private static final String LUCENE_DIRECTORY = "lucene";
    private static final String ARCHIVE_DIRECTORY = "archive";

    @Override
    protected void configureServlets() {
        bind(ArchiveNameParser.class);
        bind(IIIFPresentationRequestParser.class);
        bind(PresentationUris.class);
        bind(PresentationTransformer.class).to(PresentationTransformerImpl.class);
        bind(PresentationSerializer.class).to(JsonldSerializer.class);
        
        Names.bindProperties(binder(), loadProperties(SERVLET_CONFIG_PATH));
         
        serve("/*").with(IIIFPresentationServlet.class);
    }

    private Properties loadProperties(String path) {
        Properties props = new Properties();

        try (InputStream in = getClass().getResourceAsStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }

        return props;
    }

    @Provides
    Store provideStore(SerializerSet serializers,
            BookChecker bookChecker, BookCollectionChecker collectionChecker) {
        Path archive_path = get_webapp_path().resolve(ARCHIVE_DIRECTORY);
        LOG.info("Loading archive :: " + archive_path);
        
        ByteStreamGroup base = new FSByteStreamGroup(archive_path);
        return new StoreImpl(serializers, bookChecker, collectionChecker, base, false);
    }

    @Provides
    SimpleStore provideSimpleStore(Store store) {
        return new SimpleCachingStore(store, 1000);
    }

    @Provides
    @Named("fsi.share.map")
    Map<String, String> provideImageAlises() {
        Map<String, String> result = new HashMap<>();

        Properties props = loadProperties(FSI_SHARE_MAP_CONFIG_PATH);

        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }

        return result;
    }

    @Provides
    IIIFPresentationService providesIIIFPresentationService(SimpleStore store, PresentationSerializer jsonld_serializer,
                                    PresentationTransformer transformer) {
        return new ArchiveIIIFPresentationService(store, jsonld_serializer, transformer, 1000);
    }
    
    @Provides
    ImageIdMapper provideImageIdMapper(@Named("fsi.share.map") Map<String, String> fsi_share_map) {
        return new JhuImageIdMapper(fsi_share_map);
    }
    
    @Provides @Named("formatter.presentation")
    IIIFPresentationRequestFormatter provideIIIFPresentationRequestFormatter(@Named("iiif.pres.scheme") String scheme,
                                                             @Named("iiif.pres.host") String host,
                                                             @Named("iiif.pres.prefix") String prefix,
                                                             @Named("iiif.pres.port") int port) {
        return new IIIFPresentationRequestFormatter(scheme, host, prefix, port);
    }
    
    @Provides
    rosa.iiif.image.core.IIIFRequestFormatter provideImageRequestFormatter(@Named("iiif.image.scheme") String scheme,
                                                                           @Named("iiif.image.host") String host,
                                                                           @Named("iiif.image.prefix") String prefix,
                                                                           @Named("iiif.image.port") int port) {
        return new rosa.iiif.image.core.IIIFRequestFormatter(scheme, host, port, prefix);
    }
    
    // Derive the web app path from location of iiif-servlet.properties    
    private Path get_webapp_path() {
        try {
            return Paths.get(getClass().getResource(SERVLET_CONFIG_PATH).toURI()).getParent().getParent();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed find webapp path", e);
        }
    }
    
    @Provides
    JHSearchService provideJHSearchService(@Named("formatter.presentation") IIIFPresentationRequestFormatter requestFormatter) {
        try {
            Path index_path = get_webapp_path().resolve(LUCENE_DIRECTORY);
            LOG.info("Using lucene index path :: " + index_path);
            
            return new LuceneJHSearchService(index_path, requestFormatter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create LuceneIIIFSearchService", e);
        }
    }
}
