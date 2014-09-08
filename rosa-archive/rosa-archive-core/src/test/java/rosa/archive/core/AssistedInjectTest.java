package rosa.archive.core;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.store.DefaultStore;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.BookCollection;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class AssistedInjectTest {

    @Inject
    private StoreFactory storeFactory;
    private ByteStreamGroup bsg;

    @Before
    public void setup() {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);

        Path path = Paths.get(u.getPath()).getParent().getParent();
        
        bsg = ByteStreamGroupFactory.create(path.toString());
        assertNotNull(bsg);
    }

    @Test
    public void verifyBSGInjection() {
        assertNotNull(bsg);
        assertNotNull(bsg.id());
        assertNotNull(bsg.name());
        assertEquals("test-classes", bsg.name());
    }

    @Test
    public void verifyStoreInjection() {
        Store store = storeFactory.create(bsg);

        assertNotNull(store);
        assertEquals(DefaultStore.class, store.getClass());

        // Serializers in the Store are not accessible from the outside, so testing a few
        // methods to make sure they were injected properly....
        BookCollection collection = store.loadBookCollection("data");
        assertNotNull(collection);

        assertNotNull(collection.books());
        assertEquals(3, collection.books().length);
        assertNotNull(collection.getCharacterNames());
    }

}
