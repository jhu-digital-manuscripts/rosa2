package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.inject.Injector;

import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.PresentationTransformerImpl;

public class IIIFPresentationServletConfigTest {

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
