package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.SHA1Checksum;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see SHA1ChecksumSerializer
 */
public class SHA1ChecksumSerializerTest extends BaseSerializerTest<SHA1Checksum> {

    @Before
    public void setup() {
        serializer = new SHA1ChecksumSerializer();
    }

    @Test
    public void readTest() throws IOException {
        SHA1Checksum info = loadResource(COLLECTION_NAME, BOOK_NAME, "LudwigXV7.SHA1SUM");

        assertNotNull(info);
        assertEquals(471, info.getAllIds().size());

        String hash = info.checksums().get("LudwigXV7.permission_en.html");
        assertEquals("1a7db4954a300cae8ef041e2eaf6ec7b9a04d832", hash);
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

        // Write object and get written lines
        List<String> realLines = writeObjectAndGetWrittenLines(checksum);
        for (String line : realLines) {
            String[] parts = line.split("\\s+");

            assertEquals(2, parts.length);
            assertTrue(map.containsKey(parts[1]));
            assertTrue(map.containsValue(parts[0]));

            assertEquals(map.get(parts[1]), parts[0]);
        }
    }

    @Override
    protected SHA1Checksum createObject() {
        SHA1Checksum checksum = new SHA1Checksum();
//        checksum.setId("temp.SHA1SUM");
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

        return checksum;
    }

}
