package rosa.archive.core;

import org.junit.Before;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public abstract class AbstractFileSystemTest {

    protected ByteStreamGroup base;

    @Before
    public void setup() {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);
        String url = u.getPath();

        Path path = Paths.get(url.startsWith("/") ? url.substring(1) : url).getParent().getParent();
        assertNotNull(path);

        base = new ByteStreamGroupImpl(path.toString());
    }

}
