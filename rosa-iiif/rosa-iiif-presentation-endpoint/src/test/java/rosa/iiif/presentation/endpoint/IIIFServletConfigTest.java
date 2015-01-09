package rosa.iiif.presentation.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

import rosa.iiif.presentation.core.IIIFService;
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
        assertNotNull(trans); 
        
        IIIFService service = injector.getInstance(IIIFService.class);
        assertNotNull(service);

        IIIFServlet servlet = injector.getInstance(IIIFServlet.class);
        assertNotNull(servlet);               
    }
}
