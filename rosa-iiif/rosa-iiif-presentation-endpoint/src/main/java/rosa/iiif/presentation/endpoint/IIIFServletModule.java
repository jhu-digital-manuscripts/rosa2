package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import rosa.iiif.presentation.core.ArchiveIIIFService;
import rosa.iiif.presentation.core.IIIFService;

import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

/**
 * The servlet is configured by iiif-servlet.properties and image aliases are
 * set in image-aliases.properties.
 */
public class IIIFServletModule extends ServletModule {
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";

    @Override
    protected void configureServlets() {
        Names.bindProperties(binder(), loadProperties(SERVLET_CONFIG_PATH));

        serve("/*").with(IIIFServlet.class);
    }

    private Properties loadProperties(String path) {
        Properties props = new Properties();

        try (InputStream in = getClass().getResourceAsStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }

        return props;
    }

    @Provides
    protected IIIFService provideIIIFService() {
        return new ArchiveIIIFService();
    }
}
