package rosa.archive.core.store;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class StoreImageListsIntegrationTest extends AbstractFileSystemTest {
    private static final String COLLECTION = "collection";

    @Inject
    private StoreFactory storeFactory;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Store store;

    private Path originalPath;
    private File folder;

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        URL url = getClass().getClassLoader().getResource("data/LudwigXV7");
        assertNotNull(url);
        originalPath = Paths.get(url.toURI());
        assertNotNull(originalPath);

        folder = tempFolder.newFolder("1");
//        collectionPath = Files.createDirectories(folder.toPath().resolve(COLLECTION));
//        bookPath = Files.createDirectories(collectionPath.resolve("LudwigXV7"));

        ByteStreamGroup tempGroup = new FSByteStreamGroup(folder.toString());
        assertNotNull(tempGroup);

        store = storeFactory.create(tempGroup);
        assertNotNull(store);

    }

    /**
     * Copy all files from the original path (data/LudwigXV7) to a new book in the temporary folder,
     * except for images.csv and images.crop.csv.
     *
     * @throws IOException
     */
    private void copyTestFiles(Path collectionPath, Path bookPath) throws IOException {
        // Copy test files to tmp directory
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(originalPath, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isRegularFile(entry) && !entry.getFileName().toString().contains("images");
            }
        })) {
            for (Path path : ds) {
                try (InputStream in = Files.newInputStream(path)) {
                    String name = path.getFileName().toString();
                    Path filePath = bookPath.resolve(name);

                    Files.copy(in, filePath);
                    assertTrue(Files.exists(filePath));
                    assertTrue(Files.isRegularFile(filePath));
                }
            }

            assertEquals(1, collectionPath.toFile().list().length);
            assertEquals(51, bookPath.toFile().list().length);
        }
    }

    @Test
    public void generateAndWriteImageListIntegrationTest() throws Exception {
        Path collectionPath = Files.createDirectories(folder.toPath().resolve(COLLECTION));
        Path bookPath = Files.createDirectories(collectionPath.resolve("LudwigXV7"));

        copyTestFiles(collectionPath, bookPath);

        final String[] expected = {
                "*LudwigXV7.binding.frontcover.tif", "*LudwigXV7.frontmatter.pastedown.tif",
                "LudwigXV7.frontmatter.flyleaf.001r.tif", "*LudwigXV7.frontmatter.flyleaf.001v.tif",
                "LudwigXV7.001r.tif", "LudwigXV7.001v.tif", "LudwigXV7.002r.tif", "LudwigXV7.002v.tif",
                "LudwigXV7.003r.tif", "LudwigXV7.003v.tif", "LudwigXV7.004r.tif", "LudwigXV7.004v.tif",
                "LudwigXV7.005r.tif", "LudwigXV7.005v.tif","LudwigXV7.006r.tif", "LudwigXV7.006v.tif",
                "LudwigXV7.007r.tif", "LudwigXV7.007v.tif", "LudwigXV7.008r.tif", "LudwigXV7.008v.tif",
                "LudwigXV7.009r.tif", "LudwigXV7.009v.tif", "LudwigXV7.010r.tif", "LudwigXV7.010v.tif",
                "*LudwigXV7.endmatter.pastedown.tif", "*LudwigXV7.binding.backcover.tif"
        };

        ByteStreamGroup bookGroup = new FSByteStreamGroup(bookPath.toString());
        assertEquals(0, bookGroup.numberOfByteStreamGroups());
        assertEquals(51, bookGroup.numberOfByteStreams());
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.csv"));
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));

        List<String> errors = new ArrayList<>();
        store.generateAndWriteImageList(COLLECTION, "LudwigXV7", false, errors);

        assertTrue(errors.isEmpty());
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.csv"));

        List<String> lines = Files.readAllLines(bookPath.resolve("LudwigXV7.images.csv"), Charset.forName("UTF-8"));
        assertNotNull(lines);
        assertEquals(26, lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            assertNotNull(line);
            assertTrue(line.startsWith(expected[i]));
        }
    }

}
