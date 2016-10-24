package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
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
import rosa.iiif.presentation.core.ArchiveIIIFPresentationService;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestParser;
import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuFSIImageIdMapper;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.core.jhsearch.LuceneJHSearchService;
import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.core.transform.impl.CanvasTransformer;
import rosa.iiif.presentation.core.transform.impl.CollectionTransformer;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.core.transform.impl.LayerTransformer;
import rosa.iiif.presentation.core.transform.impl.ManifestTransformer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;
import rosa.iiif.presentation.core.transform.impl.RangeTransformer;
import rosa.iiif.presentation.core.transform.impl.SequenceTransformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;

/**
 * The servlet is configured by iiif-servlet.properties.
 */
@SuppressWarnings("unused")
public class IIIFPresentationServletModule extends ServletModule {
    private static final Logger LOG = Logger.getLogger(IIIFPresentationServletModule.class.toString());
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String FSI_SHARE_MAP_CONFIG_PATH = "/fsi-share-map.properties";

    @Override
    protected void configureServlets() {
        bind(ArchiveNameParser.class);
        bind(IIIFPresentationRequestParser.class);
        
        Multibinder<Transformer<?>> transformers = Multibinder.newSetBinder(binder(), new TypeLiteral<Transformer<?>>() {});

        bind(PresentationTransformer.class).to(PresentationTransformerImpl.class);
        bind(CollectionTransformer.class);
        bind(CanvasTransformer.class);
        bind(SequenceTransformer.class);
        bind(AnnotationTransformer.class);
        transformers.addBinding().to(AnnotationTransformer.class);
        transformers.addBinding().to(AnnotationListTransformer.class);
        transformers.addBinding().to(RangeTransformer.class);
        transformers.addBinding().to(CanvasTransformer.class);
        transformers.addBinding().to(SequenceTransformer.class);
        transformers.addBinding().to(ManifestTransformer.class);
        transformers.addBinding().to(LayerTransformer.class);

        bind(TransformerSet.class);

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
    Store provideStore(@Named("archive.path") String archive_path, SerializerSet serializers,
            BookChecker bookChecker, BookCollectionChecker collectionChecker) {
        LOG.info("Loading archive :: " + archive_path);
        ByteStreamGroup base = new FSByteStreamGroup(Paths.get(archive_path));
        return new StoreImpl(serializers, bookChecker, collectionChecker, base);
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
    IIIFPresentationService providesIIIFPresentationService(Store store, PresentationSerializer jsonld_serializer,
                                    PresentationTransformer transformer) {
        return new ArchiveIIIFPresentationService(store, jsonld_serializer, transformer, 1000);
    }
    
    @Provides
    ImageIdMapper provideImageIdMapper(@Named("fsi.share.map") Map<String, String> fsi_share_map) {
        return new JhuFSIImageIdMapper(fsi_share_map);
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
    
    @Provides
    JHSearchService provideJHSearchService(@Named("iiif.pres.search.index") String index_path,
            @Named("formatter.presentation") IIIFPresentationRequestFormatter requestFormatter) {
        LOG.info("Using lucene index path :: " + index_path);
        try {
            return new LuceneJHSearchService(Paths.get(index_path), requestFormatter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create LuceneIIIFSearchService", e);
        }
    }
}
