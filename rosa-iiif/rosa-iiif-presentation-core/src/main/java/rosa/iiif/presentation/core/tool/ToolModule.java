package rosa.iiif.presentation.core.tool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.serialize.BookDescriptionSerializer;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.core.serialize.BookReferenceSheetSerializer;
import rosa.archive.core.serialize.BookStructureSerializer;
import rosa.archive.core.serialize.CharacterNamesSerializer;
import rosa.archive.core.serialize.CropInfoSerializer;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.core.serialize.IllustrationTaggingSerializer;
import rosa.archive.core.serialize.IllustrationTitlesSerializer;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.MultilangMetadataSerializer;
import rosa.archive.core.serialize.NarrativeSectionsSerializer;
import rosa.archive.core.serialize.NarrativeTaggingSerializer;
import rosa.archive.core.serialize.PermissionSerializer;
import rosa.archive.core.serialize.ReferenceSheetSerializer;
import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.LuceneJHSearchService;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Guice module for build tools.
 */
@SuppressWarnings("unused")
public class ToolModule extends AbstractModule {
    @Override
    protected void configure() {
        Names.bindProperties(binder(), System.getProperties());
    }

    @Provides
    public Store store(SerializerSet serializerSet, BookChecker bookChecker, BookCollectionChecker collectionChecker,
            @Named("archive.path") String archive_path) {
        return new StoreImpl(serializerSet, bookChecker, collectionChecker, new FSByteStreamGroup(archive_path));
    }

    @Provides
    IIIFPresentationRequestFormatter provideIIIFPresentationRequestFormatter(@Named("iiif.pres.scheme") String scheme,
                                                                             @Named("iiif.pres.host") String host,
                                                                             @Named("iiif.pres.prefix") String prefix,
                                                                             @Named("iiif.pres.port") int port) {
        return new IIIFPresentationRequestFormatter(scheme, host, prefix, port);
    }
}
