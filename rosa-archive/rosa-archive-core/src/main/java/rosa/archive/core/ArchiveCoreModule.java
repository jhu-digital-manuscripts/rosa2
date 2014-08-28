package rosa.archive.core;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
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
import rosa.archive.core.serialize.PermissionSerializer;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
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
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;

/**
 * Dependency injection bindings for Google Guice.
 */
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
        bind(new TypeLiteral<Serializer<Transcription>>(){}).to(TranscriptionXmlSerializer.class);
        bind(new TypeLiteral<Serializer<Permission>>(){}).to(PermissionSerializer.class);

        MapBinder<Class, Serializer> mapBinder = MapBinder.newMapBinder(
                binder(),
                Class.class,
                Serializer.class
        );
        mapBinder.addBinding(BookMetadata.class).to(BookMetadataSerializer.class);
        mapBinder.addBinding(BookStructure.class).to(BookStructureSerializer.class);
        mapBinder.addBinding(CharacterNames.class).to(CharacterNamesSerializer.class);
        mapBinder.addBinding(ChecksumInfo.class).to(ChecksumInfoSerializer.class);
        mapBinder.addBinding(CropInfo.class).to(CropInfoSerializer.class);
        mapBinder.addBinding(IllustrationTagging.class).to(IllustrationTaggingSerializer.class);
        mapBinder.addBinding(IllustrationTitles.class).to(IllustrationTitlesSerializer.class);
        mapBinder.addBinding(ImageList.class).to(ImageListSerializer.class);
        mapBinder.addBinding(MissingList.class).to(MissingListSerializer.class);
        mapBinder.addBinding(NarrativeSections.class).to(NarrativeSectionsSerializer.class);
        mapBinder.addBinding(NarrativeTagging.class).to(NarrativeTaggingSerializer.class);
        mapBinder.addBinding(Transcription.class).to(TranscriptionXmlSerializer.class);
        mapBinder.addBinding(Permission.class).to(PermissionSerializer.class);

        // Data checkers

        // Store
        bind(Store.class).to(FileStore.class);
    }

}
