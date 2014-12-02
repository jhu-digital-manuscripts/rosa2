package rosa.iiif.image.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.Quality;

// TODO More robust testing

public class IIIFResponseSerializerTest {
    @Test
    public void testImageInfoToJsonLd() throws Exception {
        ImageInfo info = new ImageInfo();

        info.setImageId("testid");
        info.setImageUrl("http://example.com/iiif/testid/blah");
        info.setCompliance(ComplianceLevel.LEVEL_0);
        info.setFormats(ImageFormat.PNG);
        info.setWidth(1000);
        info.setHeight(2000);
        info.setQualities(Quality.DEFAULT, Quality.BITONAL);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new IIIFResponseSerializer().writeJsonLd(info, os);
        String result = os.toString();

        assertNotNull(result);
        assertTrue(result.contains(info.getImageId()));
    }
}
