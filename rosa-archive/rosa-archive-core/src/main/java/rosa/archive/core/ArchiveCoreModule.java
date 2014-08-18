package rosa.archive.core;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.core.serialize.BookStructureSerializer;
import rosa.archive.core.serialize.CharacterNamesSerializer;
import rosa.archive.core.serialize.ChecksumInfoSerializer;
import rosa.archive.core.serialize.CropInfoSerializer;
import rosa.archive.core.serialize.IllustrationTaggingSerializer;
import rosa.archive.core.serialize.IllustrationTitlesSerializer;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.MissingListSerializer;
import rosa.archive.core.serialize.NarrativeSectionsSerializer;
import rosa.archive.core.serialize.NarrativeTaggingSerializer;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.store.FileStore;
import rosa.archive.core.store.Store;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.MissingList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;

public class ArchiveCoreModule extends AbstractModule {

    protected void configure() {
        // Serializers
        bind(new TypeLiteral<Serializer<BookMetadata>>(){}).to(BookMetadataSerializer.class);
        bind(new TypeLiteral<Serializer<BookStructure>>(){}).to(BookStructureSerializer.class);
        bind(new TypeLiteral<Serializer<CharacterNames>>(){}).to(CharacterNamesSerializer.class);
        bind(new TypeLiteral<Serializer<ChecksumInfo>>(){}).to(ChecksumInfoSerializer.class);
        bind(new TypeLiteral<Serializer<CropInfo>>(){}).to(CropInfoSerializer.class);
        bind(new TypeLiteral<Serializer<IllustrationTagging>>(){}).to(IllustrationTaggingSerializer.class);
        bind(new TypeLiteral<Serializer<IllustrationTitles>>(){}).to(IllustrationTitlesSerializer.class);
        bind(new TypeLiteral<Serializer<ImageList>>(){}).to(ImageListSerializer.class);
        bind(new TypeLiteral<Serializer<MissingList>>(){}).to(MissingListSerializer.class);
        bind(new TypeLiteral<Serializer<NarrativeSections>>(){}).to(NarrativeSectionsSerializer.class);
        bind(new TypeLiteral<Serializer<NarrativeTagging>>(){}).to(NarrativeTaggingSerializer.class);

        // Store
        bind(Store.class).to(FileStore.class);
    }

}
