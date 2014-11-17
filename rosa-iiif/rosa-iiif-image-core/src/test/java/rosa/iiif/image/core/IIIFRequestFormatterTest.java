package rosa.iiif.image.core;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

public class IIIFRequestFormatterTest {
    private final IIIFRequestFormatter formatter = new IIIFRequestFormatter("http", "example.com", -1, "/iiif");
    private final String base = "http://example.com/iiif/";

    @Test
    public void testFormatInfoRequest() {
        InfoRequest req = new InfoRequest();

        req.setImageId("grass.png");

        // TODO Only JSON...
        req.setFormat(InfoFormat.JSON_LD);

        assertEquals(base + "grass.png/info.json", formatter.format(req));
    }

    // TODO format rotation correctly
    
    @Ignore
    public void testFormatImageRequest() {
        ImageRequest req = new ImageRequest();

        req.setImageId("gorilla");
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.BITONAL);
        req.setRegion(new Region(RegionType.FULL));
        req.setSize(new Size(SizeType.FULL));
        req.setRotation(new Rotation(90));
        
        assertEquals(base + "gorilla/full/full/90/bitonal.png", formatter.format(req));
    }
}
