package rosa.archive.core.serialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;

/**
 *
 */
public abstract class BaseSerializerTest {

    protected List<String> errors;


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        errors = new ArrayList<>();
    }

    @Test
    public abstract void readTest() throws IOException;

    @Test
    public abstract void writeTest() throws IOException;

}
