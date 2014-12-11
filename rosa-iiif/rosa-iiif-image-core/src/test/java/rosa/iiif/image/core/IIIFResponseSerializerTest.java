package rosa.iiif.image.core;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;
import org.junit.Test;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.Quality;

// TODO More testing

public class IIIFResponseSerializerTest {
    @Test
    public void testImageInfoToJsonLd() throws Exception {
        ImageInfo info = new ImageInfo();

        info.setImageId("testid");
        info.setImageUri("http://example.com/iiif/testid/blah");
        info.setCompliance(ComplianceLevel.LEVEL_0);
        info.setFormats(ImageFormat.PNG);
        info.setWidth(1000);
        info.setHeight(2000);
        info.setQualities(Quality.DEFAULT, Quality.BITONAL);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new IIIFResponseSerializer().writeJsonLd(info, os);

        check(info, new JSONObject(os.toString()));
    }

    // Check that ImageInfo has been correctly transformed to JSON-LD.
    // TODO This is not comprehensive.
    private void check(ImageInfo info, JSONObject obj) {
        assertEquals("http://iiif.io/api/image/2/context.json", obj.getString("@context"));
        assertEquals("http://iiif.io/api/image", obj.getString("protocol"));
        assertEquals(info.getImageUri(), obj.getString("@id"));
        assertEquals(info.getWidth(), obj.getInt("width"));
        assertEquals(info.getHeight(), obj.getInt("height"));
        assertEquals(info.getCompliance().getUri(), obj.getJSONArray("profile").getString(0));
    }
}
