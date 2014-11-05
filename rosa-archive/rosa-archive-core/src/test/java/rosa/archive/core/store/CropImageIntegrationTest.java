package rosa.archive.core.store;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
public class CropImageIntegrationTest extends StoreIntegrationBase {

    /**
     * cropped/ directory does not exist, but the file *.images.crop.csv does exist.
     * The code should execute and result in 1 error and no images being cropped or
     * generated. The cropped/ directory should not be created.
     *
     * @throws IOException
     */
    @Test
    public void dontCropImagesTest() throws IOException {
        copyTestFiles(defaultPath, testBook);

        assertEquals(1, store.listBookCollections().length);
        assertEquals(1, store.listBooks(COLLECTION).length);
        assertEquals(BOOK, store.listBooks(COLLECTION)[0]);

        // Make sure cropped/ directory does not exist
        ByteStreamGroup bookGroup = new FSByteStreamGroup(testBook.toString());
        assertNotNull(bookGroup);
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));
        assertFalse(bookGroup.hasByteStreamGroup("cropped"));

        System.out.println("\n---- LudwigXV7.images.csv ----");
        try (InputStream in = bookGroup.getByteStream("LudwigXV7.images.csv")) {
            List<String> lines = IOUtils.readLines(in, "UTF-8");
            for (String line : lines) {
                System.out.println(line);
            }
        }
        System.out.println("\n ---- end ----");

        List<String> errors = new ArrayList<>();
        store.cropImages(COLLECTION, BOOK, false, errors);

        assertEquals(1, errors.size());
        assertEquals("Cropped images already exist for this book. [collection:LudwigXV7]. Force overwrite with '-force'",
                    errors.get(0));
        assertFalse(bookGroup.hasByteStreamGroup("cropped"));
    }

    /**
     * cropped/ directory does not exist, but the file *.images.crop.csv does exist.
     *
     * The code should execute and result in 1 error and all 21 images being
     * cropped and put to the cropped/ directory. The error will state that for one
     * image, no cropping information will exist. This image will be directly
     * copied to the cropped/ directory.
     *
     * @throws IOException
     */
    @Test
    public void forceImageCropDespiteCropFile() throws IOException {
        copyTestFiles(defaultPath, testBook);

        assertEquals(1, store.listBookCollections().length);
        assertEquals(1, store.listBooks(COLLECTION).length);
        assertEquals(BOOK, store.listBooks(COLLECTION)[0]);

        // Make sure cropped/ directory does not exist
        ByteStreamGroup bookGroup = new FSByteStreamGroup(testBook.toString());
        assertNotNull(bookGroup);
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.csv"));
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));
        assertFalse(bookGroup.hasByteStreamGroup("cropped"));

        System.out.println("\n---- COPY: LudwigXV7.images.csv ----");
        try (InputStream in = bookGroup.getByteStream("LudwigXV7.images.csv")) {
//            List<String> lines = IOUtils.readLines(in, "UTF-8");
            List<String> lines = Files.readAllLines(Paths.get(bookGroup.id()).resolve("LudwigXV7.images.csv"));
            for (String line : lines) {
                System.out.println(line);
            }
        }
        System.out.println("\n ---- end ----");

//        System.out.println("Byte Streams:");
//        for (String name : bookGroup.listByteStreamNames()) {
//            System.out.println(name);
//        }
//        System.out.println("Byte Stream Groups:");
//        for (String name : bookGroup.listByteStreamGroupNames()) {
//            System.out.println(name);
//        }

        // count the number of original images
        assertEquals(21, countImages(bookGroup));

        List<String> errors = new ArrayList<>();
        store.cropImages(COLLECTION, BOOK, true, errors);

//        assertEquals(1, errors.size());
//        assertEquals("Image missing from cropping information, copying old file. [LudwigXV7.frontmatter.flyleaf.001r.tif]",
//                errors.get(0));
        assertTrue(bookGroup.hasByteStreamGroup("cropped"));
        assertEquals(21, countImages(bookGroup.getByteStreamGroup("cropped")));
    }

    /**
     * Count the number of TIFF images in a byte stream group
     *
     * @param bsg byte stream group
     * @return number
     * @throws IOException
     */
    private int countImages(ByteStreamGroup bsg) throws IOException {
        int count = 0;

        for (String name : bsg.listByteStreamNames()) {
            if (name.endsWith(".tif")) {
                count++;
            }
        }

        return count;
    }

}
