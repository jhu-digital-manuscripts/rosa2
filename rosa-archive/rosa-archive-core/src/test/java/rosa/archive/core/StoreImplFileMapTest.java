package rosa.archive.core;

import org.junit.Test;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.FileMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
                Arrays.stream(folgers.getContent()).anyMatch(ArchiveConstants.FILE_MAP::equals));

        Path bookPath = getBookPath(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
        testFileMap(bookPath);
    }

    /**
     * Generate a file map using image files with tabs at the end of the file names.
     *
     * @throws Exception .
     */
    @Test
    public void generateFileMapWithSpaces() throws Exception {
        List<String> errors = new ArrayList<>();
        Path bookPath = getBookPath(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);

        Files.list(bookPath).filter(file -> file.toString().trim().endsWith(".tif"))
                .forEach(file -> {
                    Path target = Paths.get(file.toString() + "\t\t");

                    try {
                        Files.move(file, target);
                    } catch (IOException e) {
                        System.err.println("Failed to rename files.");
                        fail();
                    }
                });

        store.generateFileMap(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, NEW_ID, HAS_FRONT_COVER, HAS_BACK_COVER,
                NUM_FRONTMATTER, NUM_ENDMATTER, NUM_MISC, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        // Load the book after modification
        Book ludwig = loadValidLudwigXV7();
        assertNotNull("Could not find FolgersHa2 in test archive.", ludwig);
        assertNotNull("Failed to load book content.", ludwig.getContent());

        assertTrue("File map not found in book content.",
                Arrays.stream(ludwig.getContent()).anyMatch(ArchiveConstants.FILE_MAP::equals));

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
        FileMap map;
        try (InputStream in = Files.newInputStream(fileMapPath)) {
            map = serializer.read(in, errors);
        }

        assertTrue("Unexpected errors found while reading the file map.", errors.isEmpty());
        assertNotNull("Failed to load file map.", map);
        assertNotNull(map.getMap());

        Set<String> keySet = map.getMap().keySet();

        // Make sure all images are present in file map
        Files.list(bookPath)
                .filter(file -> file.getFileName().toString().trim().endsWith(ArchiveConstants.TIF_EXT))
                .forEach(file -> assertTrue("Image not present in file map.", keySet.contains(file.getFileName().toString())));

        // Ensure no duplicate values
        assertEquals("Duplicate values found in file map which could result in some files overwriting others.",
                map.getMap().size(),
                map.getMap().entrySet().stream().distinct().count());
    }

}
