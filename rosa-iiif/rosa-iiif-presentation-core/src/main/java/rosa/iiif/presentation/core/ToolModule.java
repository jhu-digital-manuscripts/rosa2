package rosa.iiif.presentation.core;

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
import rosa.iiif.presentation.core.jhsearch.LuceneJHSearchService;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Guice module for indexing tool.
 */
@SuppressWarnings("unused")
public class ToolModule extends AbstractModule {

    @Override
    protected void configure() {
        Names.bindProperties(binder(), loadSystemProperties());

        //  Serializers
        Multibinder<Serializer<?>> serializers = Multibinder.newSetBinder(binder(), new TypeLiteral<Serializer<?>>(){});

        serializers.addBinding().to(BookMetadataSerializer.class);
        serializers.addBinding().to(BookStructureSerializer.class);
        serializers.addBinding().to(CharacterNamesSerializer.class);
        serializers.addBinding().to(SHA1ChecksumSerializer.class);
        serializers.addBinding().to(CropInfoSerializer.class);
        serializers.addBinding().to(IllustrationTaggingSerializer.class);
        serializers.addBinding().to(IllustrationTitlesSerializer.class);
        serializers.addBinding().to(ImageListSerializer.class);
        serializers.addBinding().to(NarrativeSectionsSerializer.class);
        serializers.addBinding().to(NarrativeTaggingSerializer.class);
        serializers.addBinding().to(TranscriptionXmlSerializer.class);
        serializers.addBinding().to(PermissionSerializer.class);
        serializers.addBinding().to(MultilangMetadataSerializer.class);
        serializers.addBinding().to(AORAnnotatedPageSerializer.class);
        serializers.addBinding().to(ReferenceSheetSerializer.class);
        serializers.addBinding().to(BookReferenceSheetSerializer.class);
        serializers.addBinding().to(FileMapSerializer.class);
        serializers.addBinding().to(BookDescriptionSerializer.class);

        bind(SerializerSet.class);

        // Checkers
        bind(BookChecker.class);
        bind(BookCollectionChecker.class);
    }

    private Properties loadSystemProperties() {
        return System.getProperties();
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
    public Store store(SerializerSet serializerSet, BookChecker bookChecker, BookCollectionChecker collectionChecker,
                       ByteStreamGroup archive) {
        return new StoreImpl(serializerSet, bookChecker, collectionChecker, archive);
    }

    @Provides
    public ByteStreamGroup archiveByteStreams(@Named("archive.path") String archivePath) {
        System.out.println("#### Using archive: " + archivePath);
        return new FSByteStreamGroup(archivePath);
    }

    @Provides
    SearchService provideSearchService(@Named("iiif.pres.search.index") String index_path,
                                       @Named("formatter.presentation") IIIFPresentationRequestFormatter requestFormatter) {
        try {
            return new LuceneJHSearchService(Paths.get(index_path), requestFormatter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create LuceneIIIFSearchService", e);
        }
    }

    @Provides @Named("formatter.presentation")
    IIIFPresentationRequestFormatter provideIIIFPresentationRequestFormatter(@Named("iiif.pres.scheme") String scheme,
                                                                             @Named("iiif.pres.host") String host,
                                                                             @Named("iiif.pres.prefix") String prefix,
                                                                             @Named("iiif.pres.port") int port) {
        return new IIIFPresentationRequestFormatter(scheme, host, prefix, port);
    }
}
