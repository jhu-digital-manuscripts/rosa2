package rosa.archive.core.check;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class BookCollectionIntegrationTest extends AbstractFileSystemTest {

    @Inject
    private AppConfig config;
    @Inject
    private Map<Class, Serializer> serializerMap;
    @Inject
    private StoreFactory storeFactory;

    private BookCollectionChecker checker;

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        checker = new BookCollectionChecker(config, serializerMap);
    }

    @Test
    @Ignore
    public void dontCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);

        BookCollection collection = store.loadBookCollection("rosedata");
        assertNotNull(collection);

        List<String> errors = new ArrayList<>();
        boolean check = checker.checkContent(collection, base.getByteStreamGroup("rosedata"), false, errors);
        assertTrue(check);
    }

    @Test
    @Ignore
    public void doCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);

        BookCollection collection = store.loadBookCollection("rosedata");
        assertNotNull(collection);

        List<String> errors = new ArrayList<>();
        boolean check = checker.checkContent(collection, base.getByteStreamGroup("rosedata"), true, errors);
        assertTrue(check);
    }

}
