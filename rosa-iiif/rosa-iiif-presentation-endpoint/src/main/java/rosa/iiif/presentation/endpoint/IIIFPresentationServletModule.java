package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ArchiveIIIFPresentationService;
import rosa.iiif.presentation.core.IIIFPresentationCache;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestParser;
import rosa.iiif.presentation.core.IIIFPresentationService;
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

    @Override
    protected void configureServlets() {
        bind(ArchiveNameParser.class);
        bind(IIIFPresentationRequestParser.class);
        bind(PresentationTransformer.class).to(PresentationTransformerImpl.class);
        bind(PresentationSerializer.class).to(JsonldSerializer.class);
        
        Names.bindProperties(binder(), loadProperties(Util.SERVLET_CONFIG_PATH));
         
        serve("/data/*").with(DataServlet.class);
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
        Path archive_path = Util.getArchivePath();
        LOG.info("Loading archive :: " + archive_path);
        
        ByteStreamGroup base = new FSByteStreamGroup(archive_path);
        return new StoreImpl(serializers, bookChecker, collectionChecker, base, false);
    }
    
    @Provides
    IIIFPresentationCache provideIIIFPresentationCache(Store store) {
            return new IIIFPresentationCache(store, 5000);
    }

    @Provides
    IIIFPresentationService providesIIIFPresentationService(IIIFPresentationCache cache, PresentationSerializer jsonld_serializer,
                                    PresentationTransformer transformer) {
        return new ArchiveIIIFPresentationService(cache, jsonld_serializer, transformer);
    }
    
    @Provides
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
    
    @Provides
    PresentationUris providePresentationUris(IIIFPresentationRequestFormatter presFormatter, IIIFRequestFormatter imageFormatter) {
        return new PresentationUris(presFormatter, imageFormatter);
    }
    
    @Provides
    JHSearchService provideJHSearchService(PresentationUris pres_uris) {
        try {
            Path index_path = Util.getLucenePath();
            LOG.info("Using lucene index path :: " + index_path);
            
            return new LuceneJHSearchService(index_path, pres_uris);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create LuceneIIIFSearchService", e);
        }
    }
}
