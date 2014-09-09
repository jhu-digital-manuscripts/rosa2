package rosa.archive.core;

import com.google.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.store.StoreImpl;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.BookCollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class AssistedInjectTest extends AbstractFileSystemTest {

    @Inject
    private StoreFactory storeFactory;

    @Test
    public void verifyBSGInjection() {
        assertNotNull(base);
        assertNotNull(base.id());
        assertNotNull(base.name());
        assertEquals("test-classes", base.name());
    }

    @Test
    public void verifyStoreInjection() {
        Store store = storeFactory.create(base);

        assertNotNull(store);
        assertEquals(StoreImpl.class, store.getClass());

        // Serializers in the Store are not accessible from the outside, so testing a few
        // methods to make sure they were injected properly....
        BookCollection collection = store.loadBookCollection("data");
        assertNotNull(collection);

        assertNotNull(collection.books());
        assertEquals(3, collection.books().length);
        assertNotNull(collection.getCharacterNames());
    }

}
