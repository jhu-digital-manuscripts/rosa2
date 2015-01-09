package rosa.iiif.image.core;

import static org.junit.Assert.assertEquals;

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
    private final IIIFRequestFormatter formatter = new IIIFRequestFormatter("http", "example.com", 80, "/iiif");
    private final String base = "http://example.com/iiif/";

    @Test
    public void testFormatInfoRequest() {
        InfoRequest req = new InfoRequest();

        req.setImageId("grass.png");
        req.setFormat(InfoFormat.JSON_LD);

        assertEquals(base + "grass.png/info.json", formatter.format(req));
    }
    
    @Test
    public void testFormatImageId() {
        assertEquals(base + "moo", formatter.format("moo"));
    }

    @Test
    public void testFormatImageRequest() {
        ImageRequest req = new ImageRequest();

        req.setImageId("gorilla");
        req.setFormat(ImageFormat.PNG);
        req.setQuality(Quality.BITONAL);
        req.setRegion(new Region(RegionType.FULL));
        req.setSize(new Size(SizeType.FULL));
        req.setRotation(new Rotation(90));

        // In particular check rotation formatting

        assertEquals(base + "gorilla/full/full/90/bitonal.png", formatter.format(req));

        req.setRotation(new Rotation(0));
        assertEquals(base + "gorilla/full/full/0/bitonal.png", formatter.format(req));

        req.setRotation(new Rotation(120.500));
        assertEquals(base + "gorilla/full/full/120.5/bitonal.png", formatter.format(req));

        req.setRotation(new Rotation(.05));
        assertEquals(base + "gorilla/full/full/0.05/bitonal.png", formatter.format(req));
    }

}
