package rosa.archive.core.serialize;

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class BaseSerializerTest {

    protected List<String> errors;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        // TODO loading resources
    }

}
