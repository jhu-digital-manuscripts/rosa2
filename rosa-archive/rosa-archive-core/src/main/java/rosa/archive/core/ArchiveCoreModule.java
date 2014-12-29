package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.core.serialize.BookStructureSerializer;
import rosa.archive.core.serialize.CharacterNamesSerializer;
import rosa.archive.core.serialize.CropInfoSerializer;
import rosa.archive.core.serialize.IllustrationTaggingSerializer;
import rosa.archive.core.serialize.IllustrationTitlesSerializer;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.NarrativeSectionsSerializer;
import rosa.archive.core.serialize.NarrativeTaggingSerializer;
import rosa.archive.core.serialize.PermissionSerializer;
import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Dependency injection bindings for Google Guice.
 */
public class ArchiveCoreModule extends AbstractModule {

    protected void configure() {
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
        
        bind(SerializerSet.class);
        
        // Checkers
        bind(BookChecker.class);
        bind(BookCollectionChecker.class);

        Names.bindProperties(binder(), getProperties());
    }

    private Properties getProperties() {
        Properties props = new Properties();

         // TODO redo properties
        
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("file-archive.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return props;
    }
}
