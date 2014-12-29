package rosa.archive.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreImpl;

/**
 * Setup Guice injection and a store which points at the data in src/test/resources.
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public abstract class BaseGuiceTest {
    @Inject
    protected SerializerSet serializers;

    @Inject
    protected BookChecker bookChecker;

    @Inject
    protected BookCollectionChecker collectionChecker;

    @Inject
    protected AppConfig config;

    protected ByteStreamGroup base;
    protected Store store;

    @Before
    public void setup() throws URISyntaxException, IOException {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);

        Path path = Paths.get(u.toURI()).getParent().getParent();
        assertNotNull(path);
        assertTrue(Files.isDirectory(path));

        base = new FSByteStreamGroup(path);

        store = new StoreImpl(serializers, bookChecker, collectionChecker, config, base);
    }
}
