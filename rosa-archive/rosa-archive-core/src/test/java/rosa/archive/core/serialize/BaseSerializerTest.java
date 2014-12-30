package rosa.archive.core.serialize;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rosa.archive.core.ArchiveConfig;

/**
 *
 */
public abstract class BaseSerializerTest {

    protected List<String> errors;
    @Mock
    protected ArchiveConfig config;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(config.getEncoding()).thenReturn("UTF-8");
        when(config.getLanguages()).thenReturn(new String[] {"en", "fr"});

        errors = new ArrayList<>();
    }

    @Test
    public abstract void readTest() throws IOException;

    @Test
    public abstract void writeTest() throws IOException;

}
