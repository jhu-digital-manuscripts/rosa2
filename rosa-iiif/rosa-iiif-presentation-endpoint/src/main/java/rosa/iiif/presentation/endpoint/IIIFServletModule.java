package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.iiif.presentation.core.ArchiveIIIFService;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFService;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.JhuFsiImageIdMapper;
import rosa.iiif.presentation.core.transform.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.JsonldSerializer;
import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import rosa.iiif.presentation.core.transform.RangeTransformer;

/**
 * The servlet is configured by iiif-servlet.properties.
 */
public class IIIFServletModule extends ServletModule {
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String FSI_SHARE_MAP_CONFIG_PATH = "/fsi-share-map.properties";

    @Override
    protected void configureServlets() {
        bind(PresentationTransformer.class);
        bind(PresentationSerializer.class).to(JsonldSerializer.class);
        bind(AnnotationListTransformer.class);
        bind(RangeTransformer.class);
        
        Names.bindProperties(binder(), loadProperties(SERVLET_CONFIG_PATH));
         
        serve("/*").with(IIIFServlet.class);
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
    protected Store provideStore(@Named("archive.path") String archive_path, SerializerSet serializers,
            BookChecker bookChecker, BookCollectionChecker collectionChecker) {
        ByteStreamGroup base = new FSByteStreamGroup(Paths.get(archive_path));
        return new StoreImpl(serializers, bookChecker, collectionChecker, base);
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

    @Provides
    IIIFService providesIIIFService(Store store, PresentationSerializer jsonld_serializer,
                                    PresentationTransformer transformer) {
        return new ArchiveIIIFService(store, jsonld_serializer, transformer, 1000);
    }
    
    @Provides
    ImageIdMapper provideImageIdMapper(@Named("fsi.share.map") Map<String, String> fsi_share_map) {
        return new JhuFsiImageIdMapper(fsi_share_map);
    }
    
    @Provides
    IIIFRequestFormatter providePresentationRequestFormatter(@Named("iiif.pres.scheme") String scheme, @Named("iiif.pres.host") String host, @Named("iiif.pres.prefix") String prefix, @Named("iiif.pres.port") int port) {
        return new IIIFRequestFormatter(scheme, host, prefix, port);
    }
    
    @Provides
    rosa.iiif.image.core.IIIFRequestFormatter provideImageRequestFormatter(@Named("iiif.image.scheme") String scheme, @Named("iiif.image.host") String host, @Named("iiif.image.prefix") String prefix, @Named("iiif.image.port") int port) {
        return new rosa.iiif.image.core.IIIFRequestFormatter(scheme, host, port, prefix);
    }
}
