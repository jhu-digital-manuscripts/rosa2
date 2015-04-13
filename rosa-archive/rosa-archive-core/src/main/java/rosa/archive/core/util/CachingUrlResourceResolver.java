package rosa.archive.core.util;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An instance of LSResourceResolver to be used while validating XML
 * against a schema. This resource resolver does not cache the schema,
 * instead it caches any other external calls, such as DTDs. A cache hit
 * will return immediately. If not in cache, local storage will be
 * checked, or a network call will be made if necessary.
 */
public class CachingUrlResourceResolver implements LSResourceResolver, EntityResolver {
    private static final String ENCODING = "UTF-8";
    private static final int CACHE_MAX_SIZE = 100;

    private ConcurrentHashMap<String, String> resourceCache;

    /**
     * Initialize the cache.
     */
    public CachingUrlResourceResolver() {
        this.resourceCache = new ConcurrentHashMap<>();
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        // Return immediately if systemId is already present in cache
        if (resourceCache.containsKey(systemId)) {
            return new DOMInputImpl(publicId, systemId, baseURI, resourceCache.get(systemId), ENCODING);
        }

        String data = getLocalCopy(systemId);
        if (data != null) {
            addToCache(systemId, data);
        } else {
            try (InputStream in = new URL(systemId).openStream()) {

                data = IOUtils.toString(in, ENCODING);
                addToCache(systemId, data);

            } catch (IOException e) {}
        }

        return new DOMInputImpl(publicId, systemId, baseURI, data, ENCODING);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource is = new InputSource(systemId);

        if (!resourceCache.containsKey(systemId)) {
            URL url = new URL(systemId);

            String i = getLocalCopy(systemId);
            if (i == null) {
                // Make network call only if local copy does not exist
                i = IOUtils.toString(url, ENCODING);
            }

            if (resourceCache.size() > CACHE_MAX_SIZE) {
                resourceCache.clear();
            }

            resourceCache.putIfAbsent(systemId, i);
            is.setByteStream(IOUtils.toInputStream(i, ENCODING));
        }

        is.setByteStream(IOUtils.toInputStream(resourceCache.get(systemId), ENCODING));
        return is;
    }

    private void addToCache(String key, String data) {
        if (resourceCache.size() >= CACHE_MAX_SIZE) {
            resourceCache.clear();
        }

        resourceCache.putIfAbsent(key, data);
    }

    private String getLocalCopy(String systemId) {
        try {
            String[] parts = URLDecoder.decode(systemId, ENCODING).split("/");

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(parts[parts.length - 1])) {
                if (in != null) {
                    return IOUtils.toString(in, ENCODING);
                }
            } catch (IOException e) {}
        } catch (UnsupportedEncodingException e) {}

        return null;
    }
}
