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

    private Path defaultPath;
    private File folder;

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        URL url = getClass().getClassLoader().getResource("data/LudwigXV7");
        assertNotNull(url);
        defaultPath = Paths.get(url.toURI());
        assertNotNull(defaultPath);

        folder = tempFolder.newFolder("1");

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
    private void copyTestFiles(Path originalPath, Path bookPath) throws IOException {
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
        }
    }

    private void checkImageList(Path imageListPath, String[] expected) throws IOException {
        assertNotNull(expected);
        assertNotNull(imageListPath);

        assertTrue(Files.exists(imageListPath));
        assertTrue(Files.isRegularFile(imageListPath));

        List<String> lines = Files.readAllLines(imageListPath, Charset.forName("UTF-8"));
        assertNotNull(lines);
        assertEquals(expected.length, lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            assertNotNull(line);
            assertTrue(line.startsWith(expected[i]));
        }
    }

    @Test
    public void generateAndWriteImageListIntegrationTest() throws Exception {
        Path bookPath = Files.createDirectories(folder.toPath().resolve(COLLECTION).resolve("LudwigXV7"));

        copyTestFiles(defaultPath, bookPath);

        final String[] expected = {
                "*LudwigXV7.binding.frontcover.tif", "*LudwigXV7.frontmatter.pastedown.tif",
                "LudwigXV7.frontmatter.flyleaf.001r.tif", "*LudwigXV7.frontmatter.flyleaf.001v.tif",
                "LudwigXV7.001r.tif", "LudwigXV7.001v.tif", "LudwigXV7.002r.tif", "LudwigXV7.002v.tif",
                "LudwigXV7.003r.tif", "LudwigXV7.003v.tif", "LudwigXV7.004r.tif", "LudwigXV7.004v.tif",
                "LudwigXV7.005r.tif", "LudwigXV7.005v.tif", "LudwigXV7.006r.tif", "LudwigXV7.006v.tif",
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

        checkImageList(bookPath.resolve("LudwigXV7.images.csv"), expected);
    }

    @Test
    public void imageListWithNoImages() throws Exception {
        Path collectionPath = Files.createDirectories(folder.toPath().resolve(COLLECTION));
        Path bookPath = Files.createDirectories(collectionPath.resolve("LudwigXV7"));

        // Do not copy files!

        ByteStreamGroup bookGroup = new FSByteStreamGroup(bookPath.toString());
        assertEquals(0, bookGroup.numberOfByteStreamGroups());
        assertEquals(0, bookGroup.numberOfByteStreams());

        List<String> errors = new ArrayList<>();
        store.generateAndWriteImageList(COLLECTION, "LudwigXV7", false, errors);

        assertTrue(errors.isEmpty());
        assertEquals(1, bookGroup.numberOfByteStreams());

        checkImageList(bookPath.resolve("LudwigXV7.images.csv"), new String[0]);
    }

    @Test
    public void replaceExistingImageList() throws Exception {
        URL url = getClass().getClassLoader().getResource("data/Walters143");
        assertNotNull(url);
        Path originalPath = Paths.get(url.toURI());

        Path bookPath = Files.createDirectories(folder.toPath().resolve(COLLECTION).resolve("Walters143"));

        // Copy all + image list
        copyTestFiles(originalPath, bookPath);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/Walters143/Walters143.images.csv")) {
            Files.copy(in, bookPath.resolve("Walters143.images.csv"));
        }

        ByteStreamGroup bookGroup = new FSByteStreamGroup(bookPath.toString());
        assertNotNull(bookGroup);
        assertEquals(0, bookGroup.numberOfByteStreamGroups());
        assertEquals(7, bookGroup.numberOfByteStreams());

        List<String> errors = new ArrayList<>();
        store.generateAndWriteImageList(COLLECTION, "Walters143", false, errors);

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("[Walters143.images.csv] already exists. You can force this operation to update " +
                "the existing image list.", errors.get(0));

        List<String> lines = Files.readAllLines(bookPath.resolve("Walters143.images.csv"));
        assertNotNull(lines);
        assertEquals(81, lines.size());

        errors.clear();
        store.generateAndWriteImageList(COLLECTION, "Walters143", true, errors);
        
        assertTrue(errors.isEmpty());
        checkImageList(bookPath.resolve("Walters143.images.csv"), new String[0]);
    }

}
