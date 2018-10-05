package rosa.iiif.presentation.core.tool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.StaticResourceRequestFormatter;

/**
 * Guice module for build tools.
 */
public class ToolModule extends AbstractModule {
    @Override
    protected void configure() {
        Names.bindProperties(binder(), System.getProperties());
    }

    @Provides
    public Store store(SerializerSet serializerSet, BookChecker bookChecker, BookCollectionChecker collectionChecker,
            @Named("archive.path") String archive_path) {
        return new StoreImpl(serializerSet, bookChecker, collectionChecker, new FSByteStreamGroup(archive_path), true);
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
    StaticResourceRequestFormatter provideStaticResourceRequestFormatter(@Named("iiif.pres.scheme") String scheme,
                                                                         @Named("iiif.pres.host") String host,
                                                                         @Named("iiif.pres.prefix") String prefix,
                                                                         @Named("iiif.pres.port") int port) {
        return new StaticResourceRequestFormatter(scheme, host, prefix, port);
    }
}
