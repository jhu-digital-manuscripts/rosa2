package rosa.archive.core;

import org.junit.Test;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.FileMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoreImplFileMapTest extends BaseArchiveTest {
    private static final String NEW_ID = "NewId";
    private static final boolean HAS_FRONT_COVER = true;
    private static final boolean HAS_BACK_COVER = true;
    private static final int NUM_FRONTMATTER = 10;
    private static final int NUM_ENDMATTER = 6;
    private static final int NUM_MISC = 0;

    @Test
    public void generateFileMapTest() throws IOException {
        List<String> errors = new ArrayList<>();

        store.generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, NEW_ID, HAS_FRONT_COVER, HAS_BACK_COVER,
                NUM_FRONTMATTER, NUM_ENDMATTER, NUM_MISC, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        // Load the book after modification
        Book folgers = loadValidFolgersHa2();
        assertNotNull("Could not find FolgersHa2 in test archive.", folgers);
        assertNotNull("Failed to load book content.", folgers.getContent());

        assertTrue("File map not found in book content.",
                Arrays.asList(folgers.getContent()).contains(ArchiveConstants.FILE_MAP));

        Path bookPath = getBookPath(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
        testFileMap(bookPath);
    }

    private void testFileMap(Path bookPath) throws IOException {
        assertNotNull("Path to book in test archive is missing.", bookPath);

        Path fileMapPath = bookPath.resolve("filemap.csv");
        assertNotNull("Path to file map in test book not found.", fileMapPath);
        assertTrue("File map does not exist for test book.", Files.exists(fileMapPath));

        List<String> errors = new ArrayList<>();
        FileMapSerializer serializer = new FileMapSerializer();

        // Load the file map
        FileMap map = null;
        try (InputStream in = Files.newInputStream(fileMapPath)) {
            map = serializer.read(in, errors);
        }

        assertTrue("Unexpected errors found while reading the file map.", errors.isEmpty());
        assertNotNull("Failed to load file map.", map);
        assertNotNull(map.getMap());

        Set<String> keySet = map.getMap().keySet();

        // Make sure all images are present in file map
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(bookPath, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String name = entry.getFileName().toString();
                return name.endsWith(ArchiveConstants.TIF_EXT);
            }
        })) {
            for (Path p : ds) {
                assertTrue("Image not present in file map.", keySet.contains(p.getFileName().toString()));
            }
        }
    }

}
