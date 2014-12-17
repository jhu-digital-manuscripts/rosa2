package rosa.iiif.image.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

import rosa.iiif.image.core.IIIFService;

import com.google.inject.Injector;

public class IIIFServletConfigTest {

    /**
     * Ensure that expected objects can be injected.
     * 
     */
    @Test
    public void testInjection() {
        Injector injector = new IIIFServletConfig().getInjector();

        IIIFService service = injector.getInstance(IIIFService.class);
        assertNotNull(service);
        assertNotNull(service.getCompliance());

        IIIFServlet servlet = injector.getInstance(IIIFServlet.class);
        assertNotNull(servlet);
    }
}
