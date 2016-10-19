package rosa.website.core.server;

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
import rosa.search.core.LuceneMapper;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;
import rosa.website.search.WebsiteLuceneMapper;

import java.io.IOException;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class ToolModule extends AbstractModule {

    @Override
    protected void configure() {
        Names.bindProperties(binder(), System.getProperties());
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

        bind(LuceneMapper.class).to(WebsiteLuceneMapper.class);
    }

    @Provides
    public SearchService searchService(@Named("search.index.path") String indexPath, LuceneMapper mapper) {
        try {
            return new LuceneSearchService(Paths.get(indexPath), mapper);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create search service.");
        }
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

}
