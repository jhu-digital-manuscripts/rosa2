package rosa.archive.core;

import org.junit.Before;
import org.junit.runner.RunWith;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public abstract class AbstractFileSystemTest {

    protected ByteStreamGroup base;
    protected Map<Class, Serializer> serializerMap;

    @Before
    public void setup() throws URISyntaxException, IOException {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);

        Path path = Paths.get(u.toURI()).getParent().getParent();        
        assertNotNull(path);

        base = new FSByteStreamGroup(path);

        serializerMap = mockSerializers();
    }

    @SuppressWarnings("unchecked")
    private Map<Class, Serializer> mockSerializers() {
        Set<Class> classes = new HashSet<>();

        classes.add(BookMetadata.class);
        classes.add(BookStructure.class);
        classes.add(CharacterNames.class);
        classes.add(SHA1Checksum.class);
        classes.add(CropInfo.class);
        classes.add(IllustrationTagging.class);
        classes.add(IllustrationTitles.class);
        classes.add(ImageList.class);
        classes.add(NarrativeSections.class);
        classes.add(NarrativeTagging.class);
        classes.add(Transcription.class);
        classes.add(Permission.class);

        Map<Class, Serializer> serializerMap = new HashMap<>();
        try {
            for (Class c : classes) {
                Serializer s = mock(Serializer.class);
                when(s.read(any(InputStream.class), anyList())).thenReturn(c.newInstance());
                serializerMap.put(c, s);
            }
        } catch (Exception e) {
            System.out.println("...");
        }

        return serializerMap;
    }

}
