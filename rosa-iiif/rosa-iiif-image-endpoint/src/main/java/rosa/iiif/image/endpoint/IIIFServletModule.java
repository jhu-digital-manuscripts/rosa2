package rosa.iiif.image.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import rosa.iiif.image.core.FSIService;
import rosa.iiif.image.core.IIIFService;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

/**
 * The servlet is configured by iiif-servlet.properties and image aliases are
 * set in image-aliases.properties.
 */
public class IIIFServletModule extends ServletModule {
    private static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String IMAGE_ALIASES_PATH = "/image-aliases.properties";

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
    @Named("image.aliases")
    protected Map<String, String> provideImageAlises() {
        Map<String, String> result = new HashMap<String, String>();

        Properties props = loadProperties(IMAGE_ALIASES_PATH);

        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }

        return result;
    }

    @Provides
    protected IIIFService provideImageService(@Named("fsi.url") String fsi_url) {
        return new FSIService(fsi_url, 1000, 1000);
    }
}
