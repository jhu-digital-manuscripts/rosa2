package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.SHA1Checksum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @see SHA1ChecksumSerializer
 */
public class SHA1ChecksumSerializerTest extends BaseSerializerTest {
    private static final String testFile = "data/Walters143/Walters143.SHA1SUM";

    private Serializer<SHA1Checksum> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new SHA1ChecksumSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(testFile);
        assertNotNull(is);

        SHA1Checksum info = serializer.read(is, errors);

        assertNotNull(info);
        assertEquals(13, info.getAllIds().size());

        String hash = info.checksums().get("Walters143.permission_en.html");
        assertEquals("9421c9c5988b83afb28eed96d60c5611b10d6336", hash);
    }

    @Test
    public void writeTest() throws IOException {
        String id = "temp.SHA1SUM";

        // Create test data
        SHA1Checksum checksum = new SHA1Checksum();
        checksum.setId(id);
        Map<String, String> map = checksum.checksums();

        map.put("Walters143.imagetag.csv", "723ddc7d4476b1c4a4b2a704a509947413c67fd0");
        map.put("Walters143.permission_fr.html", "2c1305a900f8c95a9f820e3158c384861ca62329");
        map.put("Walters143.transcription_en.xml", "65c9afb79d8a48ec7af1e4a38be971eae2dd50f3");
        map.put("Walters143.order.txt", "9d1e73962a51cd65f7d875221c2f3467c4bdc2a1");
        map.put("Walters143.crop.txt", "bd634a44c63f6a8144c577e575a51d371c9101f0");
        map.put("Walters143.description_en.xml", "298f9d809330430ba389f9dacd2d29c27298c99e");
        map.put("Walters143.transcription.xml", "1c3ebdade52bd59e66753d3c7d67d7050730bd9c");
        map.put("Walters143.permission.xml", "cc722817d05eab67afba53f32d81e009ecda578f");
        map.put("Walters143.description.xml", "ba0cb6a3b3719deb5c9a9288a2abf0407079d12a");
        map.put("Walters143.permission_en.xml", "22e133059e91f4ea91d085de8084e376ad4bb321");
        map.put("Walters143.description_fr.xml", "3f33d7bb70d717530734c131809af99e79c8baf2");
        map.put("Walters143.permission_fr.xml", "5eb98c10331f65bc6118f1f5480cd93d618def50");
        map.put("Walters143.permission_en.html", "9421c9c5988b83afb28eed96d60c5611b10d6336");

        // Create test file to write to
        File tempChecksumFile = tempFolder.newFile(id);

        try (OutputStream out = new FileOutputStream(tempChecksumFile)) {
            serializer.write(checksum, out);
        }

        // Check written file against real file (in test resources)
        URL url = getClass().getClassLoader().getResource("data/Walters143/Walters143.SHA1SUM");
        assertNotNull(url);

        Path realChecksum = null;
        try {
            realChecksum = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            fail("Failed to resolve URI. [" + url.toString() + "]");
        }

        List<String> realLines = Files.readAllLines(realChecksum, Charset.forName("UTF-8"));
        for (String line : realLines) {
            String[] parts = line.split("\\s+");

            assertEquals(2, parts.length);
            assertTrue(map.containsKey(parts[1]));
            assertTrue(map.containsValue(parts[0]));

            assertEquals(map.get(parts[1]), parts[0]);
        }
    }

}
