package rosa.iiif.image.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

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
    protected IIIFService provideImageService(@Named("fsi.url") String fsi_url,
                                              @Named("max.image.size") int max_image_size,
                                              @Named("tile.width") int tile_width,
                                              @Named("tile.height") int tile_height,
                                              @Named("scale.factors") String scale_factors,
                                              @Named("cache.size.image.info") int cache_size) {
        Logger.getLogger("").info("Getting image service: " + fsi_url + "(" + max_image_size + ", "
                + tile_height + "x" + tile_width + ", (" + scale_factors + "), " + cache_size + ")");
        // Ensure that base URL does NOT end in '/'
        if (fsi_url.endsWith("/")) {
            fsi_url = fsi_url.substring(0, fsi_url.length() - 1);
        }

        int[] scaleFactors;
        // Split scale_factors to create array
        if (scale_factors != null && !scale_factors.isEmpty()) {
            String[] factors = scale_factors.split(",");
            scaleFactors = new int[factors.length];
            for (int i = 0; i < factors.length; i++) {
                // This will intentionally die if non-number is encountered
                try {
                    scaleFactors[i] = Integer.parseInt(factors[i]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Scale factor config contains non-number", e);
                }
            }
        } else {
            scaleFactors = new int[] {4};
        }

        return new FSIService(fsi_url, max_image_size, tile_width, tile_height, scaleFactors, cache_size);
    }
}
