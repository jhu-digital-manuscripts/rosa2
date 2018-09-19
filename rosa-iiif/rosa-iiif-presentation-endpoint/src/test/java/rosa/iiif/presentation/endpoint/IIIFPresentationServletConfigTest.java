package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;

public class IIIFPresentationServletConfigTest {
    private static String props = "archive.path = /mnt\n"
            + "iiif.pres.scheme = http\n"
            + "iiif.pres.host = rosetest.library.jhu.edu\n" 
            + "iiif.pres.port = 80\n"
            + "iiif.pres.prefix = /iiifpres\n"
            + "iiif.pres.max_cache_age = 10\n"
            + "iiif.image.scheme = http\n"
            + "iiif.image.host = rosetest.library.jhu.edu\n" 
            + "iiif.image.port = 80\n"
            + "iiif.image.prefix = /iiifimage\n";

    /**
     * Must write new 'iiifservlet.properties' with valid values because it has not been
     * filtered by maven.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        URL propsPath = getClass().getClassLoader().getResource("iiif-servlet.properties");

        if (propsPath == null) {
            fail("Failed to load 'iiif-servlet.properties'");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(propsPath.toURI()))) {
            writer.write(props);
            writer.flush();
        }
    }

    /**
     * Ensure that expected objects can be injected.
     * 
     */
    @Test
    public void testInjection() {
        Injector injector = new IIIFPresentationServletConfig().getInjector();

        PresentationUris pres_uris = injector.getInstance(PresentationUris.class);
        assertNotNull("Failed to inject PresentationUris", pres_uris);

        PresentationTransformerImpl trans = injector.getInstance(PresentationTransformerImpl.class);
        assertNotNull("Failed to inject presentation transformer.", trans);

        IIIFPresentationService service = injector.getInstance(IIIFPresentationService.class);
        assertNotNull("Failed to inject IIIF Service.", service);

        IIIFPresentationServlet servlet = injector.getInstance(IIIFPresentationServlet.class);
        assertNotNull("Failed to inject IIIF Servlet.", servlet);
    }
}
