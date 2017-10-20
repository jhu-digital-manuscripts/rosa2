package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Injector;

import org.junit.rules.TemporaryFolder;
import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IIIFPresentationServletConfigTest {
    private static String props = "archive.path = /mnt\n" +
            "iiif.pres.scheme = http\n" +
            "iiif.pres.host = rosetest.library.jhu.edu\n" +
            "iiif.pres.port = 80\n" +
            "iiif.pres.prefix = /iiif-pres\n" +
            "iiif.pres.max_cache_age = 10\n" +
            "iiif.image.scheme = http\n" +
            "iiif.image.host = rosetest.library.jhu.edu\n" +
            "iiif.image.port = 80\n" +
            "iiif.image.prefix = /iiif-image\n" ;

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    /**
     * Write new 'iiif-servlet.properties' file that will tell the Guice servlet
     * configuration to put the Lucene index into a JUnit temporary folder.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        Path tempPath = tempDir.newFolder().toPath();
        props += tempPath.toString();

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

        PresentationTransformerImpl trans = injector.getInstance(PresentationTransformerImpl.class);
        assertNotNull("Failed to inject presentation transformer.", trans);

        AnnotationListTransformer listTrans = injector.getInstance(AnnotationListTransformer.class);
        assertNotNull("Failed to inject annotation list transformer.", listTrans);
        
        IIIFPresentationService service = injector.getInstance(IIIFPresentationService.class);
        assertNotNull("Failed to inject IIIF Service.", service);

        IIIFPresentationServlet servlet = injector.getInstance(IIIFPresentationServlet.class);
        assertNotNull("Failed to inject IIIF Servlet.", servlet);
    }
}
