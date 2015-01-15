package rosa.archive.core.util;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class CachingUrlLSResourceResolver implements LSResourceResolver {
    private static final String ENCODING = "UTF-8";
    private static final int CACHE_MAX_SIZE = 100;

    private ConcurrentHashMap<String, String> resourceCache;

    public CachingUrlLSResourceResolver() {
        this.resourceCache = new ConcurrentHashMap<>();
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        // Return immediately if systemId is already present in cache
        if (resourceCache.containsKey(systemId)) {
            return new DOMInputImpl(publicId, systemId, baseURI, resourceCache.get(systemId), ENCODING);
        }

        String data = null;
        try (InputStream in = new URL(systemId).openStream()) {

            data = IOUtils.toString(in, ENCODING);

            if (resourceCache.size() >= CACHE_MAX_SIZE) {
                resourceCache.clear();
            }

            resourceCache.putIfAbsent(systemId, data);

        } catch (IOException e) {}

        return new DOMInputImpl(publicId, systemId, baseURI, data, ENCODING);
    }
}
