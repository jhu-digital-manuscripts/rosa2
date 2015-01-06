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
import rosa.archive.core.serialize.SerializerSet;

/**
 * Setup Guice injection and a store which points to the archive in
 * src/test/resources which contains the data and rosedata collections.
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

    protected ByteStreamGroup base;
    protected Path basePath;
    protected Store store;

    @Before
    public void setupArchiveStore() throws URISyntaxException, IOException {
        URL u = getClass().getClassLoader().getResource("valid");
        assertNotNull(u);

        basePath = Paths.get(u.toURI()).getParent();
        assertNotNull(basePath);
        assertTrue(Files.isDirectory(basePath));

        base = new FSByteStreamGroup(basePath);
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base);
    }
}
