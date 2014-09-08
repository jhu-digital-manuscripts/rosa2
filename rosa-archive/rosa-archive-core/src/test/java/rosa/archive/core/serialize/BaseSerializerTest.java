package rosa.archive.core.serialize;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.config.AppConfig;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 *
 */
public abstract class BaseSerializerTest {

    protected List<String> errors;
    @Mock
    protected AppConfig config;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(config.getCHARSET()).thenReturn("UTF-8");

        errors = new ArrayList<>();
    }

}
