package rosa.archive.tool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;

import java.nio.file.Paths;

public class CopyArchiveToolModule extends AbstractModule {
    @Override
    protected void configure() {
        Names.bindProperties(binder(), System.getProperties());
    }

    // SerializerSet, BookChecker, BookCollectionChecker are bound in ArchiveCoreModule
    @Provides
    public Store store(SerializerSet serializers, BookChecker bookChecker,
                       BookCollectionChecker collectionChecker, ByteStreamGroup base) {
        return new StoreImpl(serializers, bookChecker, collectionChecker, base);
    }

    @Provides
    public ByteStreamGroup storeBase(@Named("archive.path") String archivePath) {
        return new FSByteStreamGroup(Paths.get(archivePath));
    }
}
