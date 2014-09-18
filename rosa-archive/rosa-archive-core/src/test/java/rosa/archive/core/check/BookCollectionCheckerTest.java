package rosa.archive.core.check;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see rosa.archive.core.check.BookCollectionChecker
 */
public class BookCollectionCheckerTest extends AbstractFileSystemTest {
    private static final String[] bookNames = { "LudwigXV7", "Morgan948", "Senshu2", "Walters143" };

    @Test
    public void checkTest() throws Exception {

        AppConfig config = mockAppConfig();
        Map<Class, Serializer> serializerMap = mockSerializers();

        BookCollectionChecker checker = new BookCollectionChecker(config, serializerMap);
        BookCollection collection = createBookCollection();

        ByteStreamGroup bsg = base.getByteStreamGroup("rosedata");

        assertTrue(checker.checkContent(collection, bsg, false, new ArrayList<String>()));
        assertFalse(checker.checkContent(new BookCollection(), bsg, false, new ArrayList<String>()));
        
        // TODO checkBits=TRUE

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

    /**
     * Create a map of Class objects to mocks of matching Serializers.
     *
     * @return map of Serializer mocks
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Map<Class, Serializer> mockSerializers() throws Exception{
        Map<Class, Serializer> serializerMap = new HashMap<>();

        Set<Class> classes = new HashSet<>();
        classes.add(CharacterNames.class);
        classes.add(IllustrationTitles.class);
        classes.add(NarrativeSections.class);
        classes.add(IllustrationTagging.class);
        classes.add(NarrativeTagging.class);
        classes.add(MissingList.class);

        for (Class c : classes) {
            Serializer s = mock(Serializer.class);
            when(s.read(any(InputStream.class), anyList()))
                    .thenReturn(c.newInstance());
            serializerMap.put(c, s);
        }

        return serializerMap;
    }

    /**
     * Create a new {@link rosa.archive.core.config.AppConfig} mock object.
     *
     * @return AppConfig mock
     */
    private AppConfig mockAppConfig() {
        AppConfig config = mock(AppConfig.class);

        when(config.languages()).thenReturn(new String[] { "en", "fr" });
        when(config.getIMAGE_TAGGING()).thenReturn(".imagetag.csv");
        when(config.getNARRATIVE_TAGGING()).thenReturn(".nartag.csv");
        when(config.getNARRATIVE_TAGGING_MAN()).thenReturn(".nartag.txt");

        return config;
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
