package rosa.archive.core.check;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;

import rosa.archive.core.BaseGuiceTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HashAlgorithm;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

/**
 * @see rosa.archive.core.check.BookCollectionChecker
 */
public class BookCollectionCheckerTest extends BaseGuiceTest {
    private static final String[] bookNames = { "LudwigXV7", "Morgan948", "Senshu2", "Walters143" };

    // TODO Check this
    @Test
    @Ignore
    public void checkTest() throws Exception {
        BookCollectionChecker checker = new BookCollectionChecker(serializers);
        BookCollection collection = createBookCollection();

        ByteStreamGroup bsg = base.getByteStreamGroup("rosedata");
        
        assertTrue(checker.checkContent(collection, bsg, false, new ArrayList<String>(), new ArrayList<String>()));
        assertFalse(checker.checkContent(new BookCollection(), bsg, false, new ArrayList<String>(), new ArrayList<String>()));

    }

    private BookCollection createBookCollection() {
        BookCollection collection = new BookCollection();
        collection.setId("rosedata");

        collection.setLanguages(new String[] { "en", "fr" });

        // Character names
        CharacterNames names = new CharacterNames();
        names.setId("character_names.csv");
        for (int i = 0; i < 5; i++) {
            CharacterName name = new CharacterName();
            name.setId("Character" + i);
            name.addName("Name" + i, "en");
            name.addName("Name" + i, "fr");

            names.addCharacterName(name);
        }
        collection.setCharacterNames(names);

        // Illustration titles
        IllustrationTitles titles = new IllustrationTitles();
        titles.setId("illustration_titles.csv");
        Map<String, String> titleMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            titleMap.put("Illustration" + i, "Illustration Title");
        }
        titles.setData(titleMap);
        collection.setIllustrationTitles(titles);

        // Narrative sections
        NarrativeSections sections = new NarrativeSections();
        sections.setId("narrative_sections.csv");
        List<NarrativeScene> scenes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            NarrativeScene scene = new NarrativeScene();
            scene.setId("Scene" + i);

            scenes.add(scene);
        }
        sections.setScenes(scenes);
        collection.setNarrativeSections(sections);

        collection.setBooks(bookNames);

        return collection;
    }


// ---------------------------------------------------------------------------------------------------

    @Ignore
    @Test
    public void commonsCodecDigestTest() throws Exception {
        final int MAX_ITERATION = 10000;

        MessageDigest md = DigestUtils.getDigest("SHA1");
        long apacheTime = 0;
        long customTime = 0;

        String apacheCS;
        String customCS;

        for (int i = 0; i < MAX_ITERATION; i++) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("rosedata/character_names.csv")) {
                long start = System.nanoTime();
                DigestUtils.updateDigest(md, in);
                apacheCS = bytesToHex(md.digest());
                apacheTime += System.nanoTime() - start;
            }

            try (InputStream in = getClass().getClassLoader().getResourceAsStream("rosedata/character_names.csv")) {
                long start = System.nanoTime();
                customCS = calculateChecksum(in, HashAlgorithm.SHA1);
                customTime += System.nanoTime() - start;
            }

            assertTrue(
                    "Checksum from Apache commons codec must be the same as the checksum from the custom method.",
                    apacheCS.equals(customCS)
            );
        }

        apacheTime = apacheTime / MAX_ITERATION;
        customTime = customTime / MAX_ITERATION;

        System.out.println("Apache time: " + apacheTime);
        System.out.println("Custom time: " + customTime);

    }

    protected String calculateChecksum(InputStream in, HashAlgorithm algorithm)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance(algorithm.toString());

        byte[] buff = new byte[1024];

        int numRead;
        do {
            numRead = in.read(buff);
            if (numRead > 0) {
                md.update(buff, 0, numRead);
            }
        } while (numRead != -1);

        return bytesToHex(md.digest());
    }

    private String bytesToHex(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

}
