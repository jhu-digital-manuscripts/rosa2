package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

/**
 * Test store handling of image lists.
 */

// TODO Update ignored tests when image name formatting is redone.
public class StoreImplImageListsTest extends BaseTmpStoreImplTest {
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

    @Ignore
    public void testGenImageListWithMissingImages() throws Exception {
        // Remove some images
        
        Path tmp_book_path = getTmpBookPath(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
        Path images_path = tmp_book_path.resolve("LudwigXV7.images.csv");

        System.err.println(Files.getLastModifiedTime(images_path));
        System.err.println(Files.exists(images_path));
        System.err.println(Files.size(images_path));
        
        ImageList expected = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getImages();
        
        String[] missing_images = { "LudwigXV7.003r.tif", "LudwigXV7.003v.tif", "LudwigXV7.004r.tif", "LudwigXV7.005v.tif",
                "LudwigXV7.006r.tif", "LudwigXV7.007v.tif" };

        removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, "LudwigXV7.images.csv");
        removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, "LudwigXV7.images.crop.csv");
        
        for (String missing: missing_images) {
            removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, missing);
            
            for (BookImage img: expected) {
                if (img.getId().equals(missing)) {
                    img.setMissing(true);
                }
            }
        }
        
        ByteStreamGroup bookGroup = new FSByteStreamGroup(tmp_book_path);
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.csv"));
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));

        for (String missing: missing_images) {
            assertFalse(bookGroup.hasByteStream(missing));
        }
        
        List<String> errors = new ArrayList<>();
        tmpStore.generateAndWriteImageList(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, false, errors);
        assertTrue(errors.isEmpty());

        System.err.println(Files.getLastModifiedTime(images_path));
        System.err.println(Files.exists(images_path));
        System.err.println(Files.size(images_path));
        
        ImageList result = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getImages();
        
        assertEquals(expected.getImages().size(), result.getImages().size());
        
        for (int i = 0; i < expected.getImages().size(); i++) {
            System.err.println(i);
            assertEquals(expected.getImages().get(i), result.getImages().get(i));
        }
        
        assertEquals(expected, result);
    }

    @Ignore
    public void testGenImageList() throws Exception {
        ImageList expected = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getImages();
        
        // Remove existing image lists
        
        removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, "LudwigXV7.images.csv");
        removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, "LudwigXV7.images.crop.csv");

        Path tmp_book_path = getTmpBookPath(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);

        ByteStreamGroup bookGroup = new FSByteStreamGroup(tmp_book_path);
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.csv"));
        assertFalse(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));

        List<String> errors = new ArrayList<>();
        tmpStore.generateAndWriteImageList(VALID_COLLECTION, "LudwigXV7", false, errors);
        assertTrue(errors.isEmpty());
        
        ImageList result = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getImages();
        
        assertEquals(expected, result);
    }

    /**
     * If a book archive has zero images, an image list will be generated. It
     * will contain only those six required images, which will be labeled as
     * missing.
     */
    @Test
    public void testGenImageListWithNoImages() throws Exception {
        // Create empty book
        Path book_path = getTmpBookPath(VALID_COLLECTION, "New");
        Files.createDirectory(book_path);

        ByteStreamGroup bsg = new FSByteStreamGroup(book_path);
        assertEquals(0, bsg.numberOfByteStreamGroups());
        assertEquals(0, bsg.numberOfByteStreams());

        List<String> errors = new ArrayList<>();
        tmpStore.generateAndWriteImageList(VALID_COLLECTION, "New", false, errors);
        assertTrue(errors.isEmpty());
        assertEquals(1, bsg.numberOfByteStreams());

        checkImageList(book_path.resolve("New.images.csv"), new String[] { "*New.binding.frontcover.tif",
                "*New.frontmatter.pastedown.tif", "*New.frontmatter.flyleaf.001r.tif",
                "*New.frontmatter.flyleaf.001v.tif", "*New.endmatter.pastedown.tif", "*New.binding.backcover.tif" });
    }
}
