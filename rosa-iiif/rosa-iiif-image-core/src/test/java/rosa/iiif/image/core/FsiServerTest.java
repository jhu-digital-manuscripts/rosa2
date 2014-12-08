package rosa.iiif.image.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

public class FsiServerTest {
    private FSIServer server = new FSIServer("http://fsiserver.library.jhu.edu/server", 100);

    /**
     * Test looking up an image. Disabled by default because it relies on a
     * running FSI server.
     */
    @Test
    @Ignore
    public void testLookupImage() throws Exception {
        ImageInfo info = server.lookupImage("rose/Douce195/Douce195.001r.tif");

        assertNotNull(info);
        assertNotNull(info.getCompliance());
        assertNotNull(info.getProfiles());
        assertTrue(info.getWidth() > 0);
        assertTrue(info.getHeight() > 0);

        info = server.lookupImage("doesnotexist.tif");

        assertNull(info);
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

    /**
     * Test constructing a FSI request to handle a FSI request. Testing the
     * actual images returned by FSI would be an integration test.
     * 
     * @throws Exception
     */
    @Test
    public void testConstructURL() throws Exception {
        ImageRequest req = new ImageRequest();

        Region region = new Region();
        Size scale = new Size();
        Rotation rot = new Rotation(0);

        req.setSize(scale);
        req.setRegion(region);
        req.setImageId("rose/Douce195/Douce195.001r.tif");
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.DEFAULT);
        scale.setSizeType(SizeType.FULL);
        region.setRegionType(RegionType.FULL);
        req.setRotation(rot);

        server.constructURL(req);

        req.setFormat(ImageFormat.JPG);
        req.setQuality(Quality.COLOR);

        scale.setSizeType(SizeType.EXACT);
        scale.setWidth(300);
        scale.setHeight(300);

        region.setRegionType(RegionType.ABSOLUTE);
        region.setX(100);
        region.setY(100);
        region.setWidth(600);
        region.setHeight(600);

        server.constructURL(req);

        scale.setSizeType(SizeType.PERCENTAGE);
        scale.setPercentage(0.5);

        region.setRegionType(RegionType.ABSOLUTE);
        region.setX(100);
        region.setY(100);
        region.setWidth(600);
        region.setHeight(600);

        server.constructURL(req);

        scale.setSizeType(SizeType.EXACT_WIDTH);
        scale.setWidth(200);

        region.setRegionType(RegionType.FULL);

        server.constructURL(req);

        scale.setSizeType(SizeType.EXACT_HEIGHT);
        scale.setHeight(300);

        region.setRegionType(RegionType.FULL);

        server.constructURL(req);

        rot.setMirrored(true);

        scale.setSizeType(SizeType.BEST_FIT);
        scale.setWidth(200);
        scale.setHeight(200);

        region.setRegionType(RegionType.ABSOLUTE);

        server.constructURL(req);
    }
}
