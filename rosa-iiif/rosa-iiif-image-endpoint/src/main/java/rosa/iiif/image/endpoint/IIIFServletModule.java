package rosa.iiif.image.endpoint;

import rosa.iiif.image.core.ImageServer;

import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;

public class IIIFServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        serve("/*").with(IIIFServlet.class);
    }

    @Provides
    ImageServer provideImageServer() {
        // TODO
        return null;
    }
}
