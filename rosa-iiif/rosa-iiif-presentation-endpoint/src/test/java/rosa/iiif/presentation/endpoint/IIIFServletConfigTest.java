package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

import rosa.iiif.presentation.core.IIIFService;
import rosa.iiif.presentation.core.transform.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;

import com.google.inject.Injector;

public class IIIFServletConfigTest {

    /**
     * Ensure that expected objects can be injected.
     * 
     */
    @Test
    public void testInjection() {
        Injector injector = new IIIFServletConfig().getInjector();

        PresentationTransformer trans = injector.getInstance(PresentationTransformer.class);
        assertNotNull("Failed to inject presentation transformer.", trans);

        AnnotationListTransformer listTrans = injector.getInstance(AnnotationListTransformer.class);
        assertNotNull("Failed to inject annotation list transformer.", listTrans);
        
        IIIFService service = injector.getInstance(IIIFService.class);
        assertNotNull("Failed to inject IIIF Service.", service);

        IIIFServlet servlet = injector.getInstance(IIIFServlet.class);
        assertNotNull("Failed to inject IIIF Servlet.", servlet);
    }
}
