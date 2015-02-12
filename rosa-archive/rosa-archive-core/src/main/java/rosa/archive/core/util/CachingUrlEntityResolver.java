package rosa.archive.core.util;

import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An EntityResolver that can be used with an XML parser. This EntityResolver
 * will cache external calls for entities, such as DTDs or Schema in memory.
 * When a request for an entity reaches this entity resolver, it is the
 * request is immediately filled if the requested URL is already present in
 * cache. If it is not present in cache, it first checks a known location in
 * local storage. If found here, it will be cached in memory. Finally, if these
 * two locations do not have the requested URL, an call will be made over the
 * network. The result will be cached for future use.
 */
public class CachingUrlEntityResolver implements EntityResolver {
    private static final String ENCODING = "UTF-8";
    private static final int CACHE_MAX_SIZE = 1000;

    private ConcurrentHashMap<String, String> entityCache;

    /**
     * Initialize the cache.
     */
    public CachingUrlEntityResolver() {
        this.entityCache = new ConcurrentHashMap<>();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource is = new InputSource(systemId);

        if (!entityCache.containsKey(systemId)) {
            URL url = new URL(systemId);

            String i = getLocalCopy(url);
            if (i == null) {
                // Make network call only if local copy does not exist
                i = IOUtils.toString(url, ENCODING);
            }

            if (entityCache.size() > CACHE_MAX_SIZE) {
                entityCache.clear();
            }

            entityCache.putIfAbsent(systemId, i);
            is.setByteStream(IOUtils.toInputStream(i, ENCODING));
        }

        is.setByteStream(IOUtils.toInputStream(entityCache.get(systemId), ENCODING));
        return is;
    }

    private String getLocalCopy(URL systemId) throws IOException {
        String[] parts = URLDecoder.decode(systemId.getPath(), ENCODING).split("/");

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(parts[parts.length - 1])) {
            if (in != null) {
                return IOUtils.toString(in, ENCODING);
            }
        }

        return null;
    }
}
