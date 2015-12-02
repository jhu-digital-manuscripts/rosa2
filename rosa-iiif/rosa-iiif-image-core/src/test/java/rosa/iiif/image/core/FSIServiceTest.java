package rosa.iiif.image.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

import javax.imageio.ImageIO;

import org.junit.Ignore;
import org.junit.Test;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

/**
 * Tests that rely on an external FSI server being available are ignored.
 * 
 * TODO Eventually make an IT that is enabled on the command line.
 */
public class FSIServiceTest {
    private FSIService server = new FSIService("http://fsiserver.library.jhu.edu/server", 1000, 1000, 1000,
            new int[] {1,2,4}, 100);
    private String test_image_id = "rose/Douce195/Douce195.001r.tif";
    private int test_image_width = 3732;
    private int test_image_height = 5742;

    /**
     * Test looking up an image.
     */
    @Test
    @Ignore
    public void testInfoRequest() throws Exception {
        ImageInfo info = server.perform(new InfoRequest(test_image_id));

        assertNotNull(info);
        assertNotNull(info.getCompliance());
        assertNotNull(info.getProfiles());

        assertEquals(test_image_width, info.getWidth());
        assertEquals(test_image_height, info.getHeight());
    }

    /**
     * Test looking up a non-existent image returns a 404.
     */
    @Test
    @Ignore
    public void testInfoRequestNotExist() throws Exception {
        try {
            server.perform(new InfoRequest("does not exist"));
            fail("Expected IIIFException");
        } catch (IIIFException e) {
            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, e.getHttpCode());
        }
    }

    /**
     * Test parsing FSI XML info response.
     */
    @Test
    public void testParseFsiInfo() throws Exception {
        String xml = "<fsi:FSI xmlns:fsi='http://www.fsi-viewer.com/schema'><Image><Width value='3732'/><Height value='5742'/></Image></fsi:FSI>";

        try (InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"))) {
            ImageInfo info = server.parse_image_info(is);

            assertNotNull(info);
            assertEquals(3732, info.getWidth());
            assertEquals(5742, info.getHeight());
        }
    }

    @Test
    @Ignore
    public void testPerformImageRequestExactDistorted() throws Exception {
        ImageRequest req = new ImageRequest();

        Region region = new Region();
        Size scale = new Size();
        Rotation rot = new Rotation(0);

        scale.setWidth(500);
        scale.setHeight(400);

        req.setSize(scale);
        req.setRegion(region);
        req.setImageId(test_image_id);
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.DEFAULT);
        scale.setSizeType(SizeType.EXACT);
        region.setRegionType(RegionType.FULL);
        req.setRotation(rot);

        check(req);
    }

    @Test
    @Ignore
    public void testPerformImageRequestExactWidth() throws Exception {
        ImageRequest req = new ImageRequest();

        Region region = new Region();
        Size scale = new Size();
        Rotation rot = new Rotation(0);

        scale.setWidth(200);

        req.setSize(scale);
        req.setRegion(region);
        req.setImageId(test_image_id);
        req.setFormat(ImageFormat.JPG);
        req.setQuality(Quality.COLOR);
        scale.setSizeType(SizeType.EXACT_WIDTH);
        region.setRegionType(RegionType.FULL);
        req.setRotation(rot);

        check(req);
    }

    @Test
    @Ignore
    public void testPerformImageRequestRegionAbsolute() throws Exception {
        ImageRequest req = new ImageRequest();

        Region region = new Region();
        Size scale = new Size();
        Rotation rot = new Rotation(0);

        scale.setSizeType(SizeType.EXACT);
        scale.setWidth(300);
        scale.setHeight(300);

        region.setRegionType(RegionType.ABSOLUTE);
        region.setX(100);
        region.setY(100);
        region.setWidth(600);
        region.setHeight(600);

        req.setSize(scale);
        req.setRegion(region);
        req.setImageId(test_image_id);
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.DEFAULT);
        req.setRotation(rot);

        check(req);
    }

    // Perform an image request and check that the resulting image is correct.
    // This is only partially implemented.
    private void check(ImageRequest req) throws Exception {
        // Sleep to avoid spamming image server
        Thread.sleep(1000);

        BufferedImage img;

        try (InputStream is = server.perform(req)) {
            img = ImageIO.read(is);
        }

        Size size = req.getSize();
                
        int expected_width = size.getWidth();        
        int expected_height = size.getHeight();

        if (size.getSizeType() == SizeType.EXACT_WIDTH) {
            expected_height = (int) Math.round(((double) (expected_width * test_image_height)) / test_image_width);
        }
            
        assertEquals(expected_width, img.getWidth());
        assertEquals(expected_height, img.getHeight());
    }
}
