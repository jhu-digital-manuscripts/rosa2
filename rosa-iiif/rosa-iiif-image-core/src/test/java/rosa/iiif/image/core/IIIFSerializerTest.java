package rosa.iiif.image.core;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.Quality;

// TODO
public class IIIFSerializerTest {
    @Test
    public void testImageInfoJSON() throws Exception {
        ImageInfo info = new ImageInfo();

        info.setImageId("testid");
        info.setFormats(ImageFormat.PNG);
        info.setWidth(1000);
        info.setHeight(2000);
        info.setQualities(Quality.DEFAULT);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new IIIFSerializer(ComplianceLevel.LEVEL_1).toJsonLd(info, os);
    }
}
