package rosa.archive.core;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.core.serialize.BookStructureSerializer;
import rosa.archive.core.serialize.CharacterNamesSerializer;
import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.core.serialize.CropInfoSerializer;
import rosa.archive.core.serialize.IllustrationTaggingSerializer;
import rosa.archive.core.serialize.IllustrationTitlesSerializer;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.NarrativeSectionsSerializer;
import rosa.archive.core.serialize.NarrativeTaggingSerializer;
import rosa.archive.core.serialize.PermissionSerializer;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.core.store.StoreImpl;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Dependency injection bindings for Google Guice.
 */
public class ArchiveCoreModule extends AbstractModule {

    protected void configure() {
        // Serializers
        //  Single class binding: Serializer<Type> -> correct serializer implementation
        bind(new TypeLiteral<Serializer<BookMetadata>>(){}).to(BookMetadataSerializer.class);
        bind(new TypeLiteral<Serializer<BookStructure>>(){}).to(BookStructureSerializer.class);
        bind(new TypeLiteral<Serializer<CharacterNames>>(){}).to(CharacterNamesSerializer.class);
        bind(new TypeLiteral<Serializer<SHA1Checksum>>(){}).to(SHA1ChecksumSerializer.class);
        bind(new TypeLiteral<Serializer<CropInfo>>(){}).to(CropInfoSerializer.class);
        bind(new TypeLiteral<Serializer<IllustrationTagging>>(){}).to(IllustrationTaggingSerializer.class);
        bind(new TypeLiteral<Serializer<IllustrationTitles>>(){}).to(IllustrationTitlesSerializer.class);
        bind(new TypeLiteral<Serializer<ImageList>>(){}).to(ImageListSerializer.class);
        bind(new TypeLiteral<Serializer<NarrativeSections>>(){}).to(NarrativeSectionsSerializer.class);
        bind(new TypeLiteral<Serializer<NarrativeTagging>>(){}).to(NarrativeTaggingSerializer.class);
        bind(new TypeLiteral<Serializer<Transcription>>(){}).to(TranscriptionXmlSerializer.class);
        bind(new TypeLiteral<Serializer<Permission>>(){}).to(PermissionSerializer.class);

        //  Multibinding: Map<Class type, Serializer> -> key=Class, value=correct serializer implementation
        MapBinder<Class, Serializer> mapBinder = MapBinder.newMapBinder(
                binder(),
                Class.class,
                Serializer.class
        );
        mapBinder.addBinding(BookMetadata.class).to(BookMetadataSerializer.class);
        mapBinder.addBinding(BookStructure.class).to(BookStructureSerializer.class);
        mapBinder.addBinding(CharacterNames.class).to(CharacterNamesSerializer.class);
        mapBinder.addBinding(SHA1Checksum.class).to(SHA1ChecksumSerializer.class);
        mapBinder.addBinding(CropInfo.class).to(CropInfoSerializer.class);
        mapBinder.addBinding(IllustrationTagging.class).to(IllustrationTaggingSerializer.class);
        mapBinder.addBinding(IllustrationTitles.class).to(IllustrationTitlesSerializer.class);
        mapBinder.addBinding(ImageList.class).to(ImageListSerializer.class);
        mapBinder.addBinding(NarrativeSections.class).to(NarrativeSectionsSerializer.class);
        mapBinder.addBinding(NarrativeTagging.class).to(NarrativeTaggingSerializer.class);
        mapBinder.addBinding(Transcription.class).to(TranscriptionXmlSerializer.class);
        mapBinder.addBinding(Permission.class).to(PermissionSerializer.class);
        mapBinder.addBinding(AnnotatedPage.class).to(AORAnnotatedPageSerializer.class);

        // Data checkers
        bind(BookChecker.class);
        bind(BookCollectionChecker.class);

        // Store
        install(new FactoryModuleBuilder()
                .implement(Store.class, StoreImpl.class)
                .build(StoreFactory.class));

        Names.bindProperties(binder(), getProperties());
    }

    private Properties getProperties() {
        Properties props = new Properties();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("file-archive.properties")) {
            props.load(in);
        } catch (IOException e) {
            // TODO log
        }

        return props;
    }

}
