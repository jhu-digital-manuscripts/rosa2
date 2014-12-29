package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;

/**
 * @see rosa.archive.core.serialize.CropInfoSerializer
 */
public class CropInfoSerializerTest extends BaseSerializerTest {

    private Serializer<CropInfo> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new CropInfoSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.crop.txt";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            CropInfo info = serializer.read(in, errors);
            assertNotNull(info);

            CropData data = info.getCropDataForPage("Walters143.039v.tif");
            double delta = 0.0000001;
            assertEquals("Walters143.039v.tif", data.getId());
            assertEquals(0.119649, data.getLeft(), delta);
            assertEquals(0.036224, data.getRight(), delta);
            assertEquals(0.036667, data.getTop(), delta);
            assertEquals(0.016667, data.getBottom(), delta);
        }
    }

    @Test
    public void writeTest() throws IOException {
        CropInfo info = createCropInfo();

        File cropFile = tempFolder.newFile();
        try (OutputStream out = Files.newOutputStream(cropFile.toPath())) {
            serializer.write(info, out);
        }

        List<String> lines = Files.readAllLines(cropFile.toPath(), Charset.forName("UTF-8"));

        assertNotNull(lines);
        assertNotEquals(0, lines.size());
        assertTrue(lines.contains("LudwigXV7.frontmatter.pastedown.tif 0.035419 0.025974 0.026667 0.023333"));
        assertTrue(lines.contains("LudwigXV7.136v.tif 0.055427 0.063510 0.040833 0.045833"));

    }

    private CropInfo createCropInfo() {
        String[] data = {
                "LudwigXV7.binding.frontcover.tif 0.036866 0.0 28802 0.016667 0.025000",
                "LudwigXV7.frontmatter.pastedown.tif 0.035419 0.025974 0.026667 0.023333",
                "LudwigXV7.frontmatter.flyleaf.01r.tif 0.036866 0.034602 0.016667 0.016667",
                "LudwigXV7.frontmatter.flyleaf.04r.tif 0.043478 0.041475 0.019167 0.024167",
                "LudwigXV7.frontmatter.flyleaf.04v.tif 0.075581 0.031395 0.040000 0.030000",
                "LudwigXV7.001r.tif 0.039216 0.066897 0.017500 0.032500",
                "LudwigXV7.001v.tif 0.084668 0.042334 0.045000 0.043333",
                "LudwigXV7.136v.tif 0.055427 0.063510 0.040833 0.045833",
                "LudwigXV7.endmatter.flyleaf.01r.tif 0.053118 0.074661 0.038333 0.031667",
                "LudwigXV7.endmatter.flyleaf.02v.tif 0.044237 0.027939 0.025000 0.021667",
                "LudwigXV7.endmatter.pastedown.tif 0.029104 0.031432 0.016667 0.028333",
                "LudwigXV7.binding.backcover.tif 0.029104 0.037515 0.024167 0.034167"
        };

        CropInfo info = new CropInfo();

        for (String line : data) {
            String[] parts = line.split("\\s+");
            CropData cData = new CropData();

            cData.setId(parts[0]);
            cData.setLeft(Double.parseDouble(parts[1]));
            cData.setRight(Double.parseDouble(parts[2]));
            cData.setTop(Double.parseDouble(parts[3]));
            cData.setBottom(Double.parseDouble(parts[4]));

            info.addCropData(cData);
        }

        return info;
    }

}
