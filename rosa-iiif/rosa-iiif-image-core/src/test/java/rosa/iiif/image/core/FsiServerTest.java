package rosa.iiif.image.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;


// TODO Mock this somehow?, expose parser of fsi xml...

public class FsiServerTest {
    private FSIServer server = new FSIServer(
            "http://fsiserver.library.jhu.edu/server");

    @Test
    public void testLookupImage() throws Exception {
        ImageInfo info = server.lookupImage("rose/Douce195/Douce195.001r.tif");

        assertNotNull(info);
        assertTrue(info.getWidth() > 0);
        assertTrue(info.getHeight() > 0);

        //System.out.println(info.getWidth() + " " + info.getHeight());

        info = server.lookupImage("moo/moo.tif");

        assertNull(info);
    }

    public void testConstructURL() throws Exception {
        ImageRequest req = new ImageRequest();

        Region region = new Region();
        Size scale = new Size();

        req.setSize(scale);
        req.setRegion(region);
        req.setImageId("rose/Douce195/Douce195.001r.tif");
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.DEFAULT);
        scale.setSizeType(SizeType.FULL);
        region.setRegionType(RegionType.FULL);

        String url = server.constructURL(req);
        //new URL(url).openStream().close();
        //System.out.println(url);

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

        url = server.constructURL(req);
        // new URL(url).openStream().close();
        //System.out.println(url);

        scale.setSizeType(SizeType.PERCENTAGE);
        scale.setPercentage(0.5);

        region.setRegionType(RegionType.ABSOLUTE);
        region.setX(100);
        region.setY(100);
        region.setWidth(600);
        region.setHeight(600);

        url = server.constructURL(req);
        // new URL(url).openStream().close();
        //System.out.println(url);

        scale.setSizeType(SizeType.EXACT_WIDTH);
        scale.setWidth(200);

        region.setRegionType(RegionType.FULL);

        url = server.constructURL(req);
        // new URL(url).openStream().close();
        System.out.println(url);

        scale.setSizeType(SizeType.EXACT_HEIGHT);
        scale.setHeight(300);

        region.setRegionType(RegionType.FULL);

        url = server.constructURL(req);
        // new URL(url).openStream().close();
        //System.out.println(url);

        scale.setSizeType(SizeType.BEST_FIT);
        scale.setWidth(200);
        scale.setHeight(200);

        region.setRegionType(RegionType.ABSOLUTE);

        url = server.constructURL(req);
        // new URL(url).openStream().close();
        //System.out.println(url);

    }
}
